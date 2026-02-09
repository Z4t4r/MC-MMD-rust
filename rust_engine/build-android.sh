#!/bin/bash
# Android ARM64 交叉编译脚本
# 用法: ./build-android.sh [--release]
#
# 前置条件:
#   1. 安装 Android NDK (r23+)，设置环境变量 ANDROID_NDK_HOME
#      例: export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/27.0.12077973
#   2. 添加 Rust target:
#      rustup target add aarch64-linux-android

set -e

RELEASE=""
PROFILE="debug"
if [[ "$1" == "--release" || "$1" == "-r" ]]; then
    RELEASE="--release"
    PROFILE="release"
fi

# ===== NDK 检查 =====
if [ -z "$ANDROID_NDK_HOME" ]; then
    echo "错误: 未设置 ANDROID_NDK_HOME 环境变量"
    echo "  export ANDROID_NDK_HOME=\$HOME/Android/Sdk/ndk/<version>"
    exit 1
fi

# 检测宿主机 OS
HOST_OS=$(uname -s | tr '[:upper:]' '[:lower:]')
case "$HOST_OS" in
    linux)  HOST_TAG="linux-x86_64" ;;
    darwin) HOST_TAG="darwin-x86_64" ;;
    *)      HOST_TAG="windows-x86_64" ;;
esac

TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/$HOST_TAG"
if [ ! -d "$TOOLCHAIN" ]; then
    echo "错误: NDK toolchain 不存在: $TOOLCHAIN"
    exit 1
fi

# API Level 24 = Android 7.0 (FCL 等启动器的最低要求)
API_LEVEL=24
CC_AARCH64="$TOOLCHAIN/bin/aarch64-linux-android${API_LEVEL}-clang"

echo "=== Android ARM64 交叉编译 ($PROFILE) ==="
echo "NDK: $ANDROID_NDK_HOME"
echo "CC:  $CC_AARCH64"

export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$CC_AARCH64"
export CC_aarch64_linux_android="$CC_AARCH64"
export AR_aarch64_linux_android="$TOOLCHAIN/bin/llvm-ar"

cargo build $RELEASE --target aarch64-linux-android

if [ $? -ne 0 ]; then
    echo "编译失败!" >&2
    exit 1
fi

# 复制产物
SRC="target/aarch64-linux-android/$PROFILE/libmmd_engine.so"
DEST_DIR="../docs"
DEST="$DEST_DIR/libmmd_engine-android-arm64.so"

mkdir -p "$DEST_DIR"
cp "$SRC" "$DEST"
echo "=== 构建完成: $DEST ==="
