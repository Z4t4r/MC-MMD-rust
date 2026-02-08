//! Morph 管理器
//!
//! 完整实现 MMD Morph 系统：
//! - Vertex Morph: 顶点位置偏移
//! - Bone Morph: 骨骼平移/旋转偏移
//! - Group Morph: 递归组合多个 Morph（含循环引用保护）
//! - Material Morph: 材质参数变形（乘算/加算）
//! - UV Morph: 纹理坐标偏移

use std::collections::HashMap;
use glam::{Vec2, Vec3, Vec4};

use crate::skeleton::BoneManager;
use super::{Morph, MorphType, MaterialMorphOffset};

/// 单个材质的 Morph 计算结果
///
/// 渲染时用此结果覆盖/调制原始材质参数。
/// 乘算基准为 1.0（无变化），加算基准为 0.0。
#[derive(Clone, Debug)]
pub struct MaterialMorphResult {
    pub diffuse: Vec4,
    pub specular: Vec3,
    pub specular_strength: f32,
    pub ambient: Vec3,
    pub edge_color: Vec4,
    pub edge_size: f32,
    pub texture_tint: Vec4,
    pub environment_tint: Vec4,
    pub toon_tint: Vec4,
}

impl Default for MaterialMorphResult {
    fn default() -> Self {
        Self {
            diffuse: Vec4::ONE,
            specular: Vec3::ONE,
            specular_strength: 1.0,
            ambient: Vec3::ONE,
            edge_color: Vec4::ONE,
            edge_size: 1.0,
            texture_tint: Vec4::ONE,
            environment_tint: Vec4::ONE,
            toon_tint: Vec4::ONE,
        }
    }
}

impl MaterialMorphResult {
    /// 重置为乘算单位值（无变化）
    pub fn reset(&mut self) {
        *self = Self::default();
    }
    
    /// 应用一个乘算偏移（operation=0）
    /// 将 lerp(1.0, offset, weight) 乘到现有累积值上，确保多个乘算 Morph 正确复合
    fn apply_multiply(&mut self, offset: &MaterialMorphOffset, weight: f32) {
        let w = weight;
        let inv_w = 1.0 - w;
        self.diffuse *= Vec4::ONE * inv_w + offset.diffuse * w;
        self.specular *= Vec3::ONE * inv_w + offset.specular * w;
        self.specular_strength *= inv_w + offset.specular_strength * w;
        self.ambient *= Vec3::ONE * inv_w + offset.ambient * w;
        self.edge_color *= Vec4::ONE * inv_w + offset.edge_color * w;
        self.edge_size *= inv_w + offset.edge_size * w;
        self.texture_tint *= Vec4::ONE * inv_w + offset.texture_tint * w;
        self.environment_tint *= Vec4::ONE * inv_w + offset.environment_tint * w;
        self.toon_tint *= Vec4::ONE * inv_w + offset.toon_tint * w;
    }
    
    /// 应用一个加算偏移（operation=1）
    fn apply_additive(&mut self, offset: &MaterialMorphOffset, weight: f32) {
        self.diffuse += offset.diffuse * weight;
        self.specular += offset.specular * weight;
        self.specular_strength += offset.specular_strength * weight;
        self.ambient += offset.ambient * weight;
        self.edge_color += offset.edge_color * weight;
        self.edge_size += offset.edge_size * weight;
        self.texture_tint += offset.texture_tint * weight;
        self.environment_tint += offset.environment_tint * weight;
        self.toon_tint += offset.toon_tint * weight;
    }
}

/// Morph 管理器
pub struct MorphManager {
    morphs: Vec<Morph>,
    name_to_index: HashMap<String, usize>,
    
    /// 每个材质的 Morph 计算结果（材质索引 -> 结果）
    material_morph_results: Vec<MaterialMorphResult>,
    /// 材质数量（由外部设置）
    material_count: usize,
    
    /// UV Morph 偏移结果（每顶点的 UV 偏移，在 apply_morphs 时累积）
    uv_morph_deltas: Vec<Vec2>,
    /// 顶点数量（由外部设置）
    vertex_count: usize,
}

impl MorphManager {
    pub fn new() -> Self {
        Self {
            morphs: Vec::new(),
            name_to_index: HashMap::new(),
            material_morph_results: Vec::new(),
            material_count: 0,
            uv_morph_deltas: Vec::new(),
            vertex_count: 0,
        }
    }
    
    /// 设置材质数量（模型加载后调用）
    pub fn set_material_count(&mut self, count: usize) {
        self.material_count = count;
        self.material_morph_results = vec![MaterialMorphResult::default(); count];
    }
    
    /// 设置顶点数量（模型加载后调用）
    pub fn set_vertex_count(&mut self, count: usize) {
        self.vertex_count = count;
        self.uv_morph_deltas = vec![Vec2::ZERO; count];
    }
    
    /// 添加 Morph
    pub fn add_morph(&mut self, morph: Morph) {
        let index = self.morphs.len();
        self.name_to_index.insert(morph.name.clone(), index);
        self.morphs.push(morph);
    }
    
    /// 通过名称查找 Morph
    pub fn find_morph_by_name(&self, name: &str) -> Option<usize> {
        self.name_to_index.get(name).copied()
    }
    
    /// 获取 Morph 数量
    pub fn morph_count(&self) -> usize {
        self.morphs.len()
    }
    
    /// 获取 Morph
    pub fn get_morph(&self, index: usize) -> Option<&Morph> {
        self.morphs.get(index)
    }
    
    /// 获取可变 Morph 引用
    pub fn get_morph_mut(&mut self, index: usize) -> Option<&mut Morph> {
        self.morphs.get_mut(index)
    }
    
    /// 设置 Morph 权重
    pub fn set_morph_weight(&mut self, index: usize, weight: f32) {
        if let Some(morph) = self.morphs.get_mut(index) {
            morph.set_weight(weight);
        }
    }
    
    /// 获取 Morph 权重
    pub fn get_morph_weight(&self, index: usize) -> f32 {
        self.morphs.get(index).map(|m| m.weight).unwrap_or(0.0)
    }
    
    /// 重置所有 Morph 权重
    pub fn reset_all_weights(&mut self) {
        for morph in &mut self.morphs {
            morph.reset();
        }
    }
    
    /// 获取材质 Morph 结果
    pub fn get_material_morph_result(&self, material_index: usize) -> Option<&MaterialMorphResult> {
        self.material_morph_results.get(material_index)
    }
    
    /// 获取所有材质 Morph 结果
    pub fn get_material_morph_results(&self) -> &[MaterialMorphResult] {
        &self.material_morph_results
    }
    
    /// 获取 UV Morph 偏移结果
    pub fn get_uv_morph_deltas(&self) -> &[Vec2] {
        &self.uv_morph_deltas
    }
    
    /// 应用所有 Morph（完整流水线）
    ///
    /// 处理顺序遵循 MMD 规范：
    /// 1. 重置材质/UV 累积缓冲区
    /// 2. 遍历所有 Morph，按类型分发处理
    /// 3. Group Morph 递归展开子项
    pub fn apply_morphs(&mut self, bone_manager: &mut BoneManager, positions: &mut [Vec3]) {
        // 重置材质 Morph 结果
        for result in &mut self.material_morph_results {
            result.reset();
        }
        // 重置 UV 偏移
        for delta in &mut self.uv_morph_deltas {
            *delta = Vec2::ZERO;
        }
        
        // 收集需要处理的 morph 索引和权重（避免借用冲突）
        let active_morphs: Vec<(usize, f32)> = self.morphs.iter().enumerate()
            .filter(|(_, m)| m.weight.abs() > 0.001)
            .map(|(i, m)| (i, m.weight))
            .collect();
        
        for (morph_idx, weight) in active_morphs {
            self.apply_single_morph(morph_idx, weight, bone_manager, positions, 0);
        }
    }
    
    /// 应用单个 Morph（支持递归，depth 用于防止无限循环）
    fn apply_single_morph(
        &mut self,
        morph_idx: usize,
        effective_weight: f32,
        bone_manager: &mut BoneManager,
        positions: &mut [Vec3],
        depth: u32,
    ) {
        // 防止无限递归（Group Morph 可能循环引用）
        if depth > 16 || effective_weight.abs() < 0.001 {
            return;
        }
        
        // 安全地获取 morph 数据的副本以避免借用冲突
        let morph_type;
        let vertex_offsets;
        let bone_offsets;
        let material_offsets;
        let uv_offsets;
        let group_offsets;
        
        if let Some(morph) = self.morphs.get(morph_idx) {
            morph_type = morph.morph_type.clone();
            vertex_offsets = morph.vertex_offsets.clone();
            bone_offsets = morph.bone_offsets.clone();
            material_offsets = morph.material_offsets.clone();
            uv_offsets = morph.uv_offsets.clone();
            group_offsets = morph.group_offsets.clone();
        } else {
            return;
        }
        
        match morph_type {
            MorphType::Vertex => {
                Self::apply_vertex_morph_static(&vertex_offsets, effective_weight, positions);
            }
            MorphType::Bone => {
                Self::apply_bone_morph_static(&bone_offsets, effective_weight, bone_manager);
            }
            MorphType::Group | MorphType::Flip => {
                for sub in &group_offsets {
                    let sub_idx = sub.morph_index as usize;
                    if sub_idx < self.morphs.len() && sub_idx != morph_idx {
                        let sub_weight = effective_weight * sub.influence;
                        self.apply_single_morph(sub_idx, sub_weight, bone_manager, positions, depth + 1);
                    }
                }
            }
            MorphType::Material => {
                self.apply_material_morph_offsets(&material_offsets, effective_weight);
            }
            MorphType::Uv | MorphType::AdditionalUv1 => {
                self.apply_uv_morph_offsets(&uv_offsets, effective_weight);
            }
            _ => {
                // AdditionalUv2/3/4, Impulse 暂不处理
            }
        }
    }
    
    /// 应用顶点 Morph（静态方法，不需要 &self）
    fn apply_vertex_morph_static(
        offsets: &[super::VertexMorphOffset],
        weight: f32,
        positions: &mut [Vec3],
    ) {
        for offset in offsets {
            let idx = offset.vertex_index as usize;
            if idx < positions.len() {
                positions[idx] += offset.offset * weight;
            }
        }
    }
    
    /// 应用骨骼 Morph（静态方法，不需要 &self）
    fn apply_bone_morph_static(
        offsets: &[super::BoneMorphOffset],
        weight: f32,
        bone_manager: &mut BoneManager,
    ) {
        for offset in offsets {
            let idx = offset.bone_index as usize;
            let translation = offset.translation * weight;
            let rotation = glam::Quat::from_xyzw(
                offset.rotation.x * weight,
                offset.rotation.y * weight,
                offset.rotation.z * weight,
                1.0 - (1.0 - offset.rotation.w) * weight,
            ).normalize();
            
            if let Some(bone) = bone_manager.get_bone_mut(idx) {
                bone.animation_translate += translation;
                bone.animation_rotate = bone.animation_rotate * rotation;
            }
        }
    }
    
    /// 应用材质 Morph 偏移
    ///
    /// material_index == -1 表示应用到所有材质。
    /// operation: 0=乘算, 1=加算
    fn apply_material_morph_offsets(
        &mut self,
        offsets: &[MaterialMorphOffset],
        weight: f32,
    ) {
        for offset in offsets {
            if offset.material_index < 0 {
                // -1 表示应用到所有材质
                for result in &mut self.material_morph_results {
                    if offset.operation == 0 {
                        result.apply_multiply(offset, weight);
                    } else {
                        result.apply_additive(offset, weight);
                    }
                }
            } else {
                let mat_idx = offset.material_index as usize;
                if mat_idx < self.material_morph_results.len() {
                    if offset.operation == 0 {
                        self.material_morph_results[mat_idx].apply_multiply(offset, weight);
                    } else {
                        self.material_morph_results[mat_idx].apply_additive(offset, weight);
                    }
                }
            }
        }
    }
    
    /// 应用 UV Morph 偏移
    fn apply_uv_morph_offsets(
        &mut self,
        offsets: &[super::UvMorphOffset],
        weight: f32,
    ) {
        for offset in offsets {
            let idx = offset.vertex_index as usize;
            if idx < self.uv_morph_deltas.len() {
                // 只取 x, y 分量作为 UV 偏移
                self.uv_morph_deltas[idx] += Vec2::new(offset.offset.x, offset.offset.y) * weight;
            }
        }
    }
}

impl Default for MorphManager {
    fn default() -> Self {
        Self::new()
    }
}
