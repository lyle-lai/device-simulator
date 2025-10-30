#!/bin/bash

# 当任何命令失败时立即退出脚本
set -e

# --- 配置 ---
IMAGE_NAME="xinsec/device-simulator"
IMAGE_TAG="1.0"
OUTPUT_FILE="device-simulator-${IMAGE_TAG}.tar.gz"
# ---------------------

echo "=================================================="
echo " 构建 Docker 镜像"
echo " 镜像: ${IMAGE_NAME}:${IMAGE_TAG}"
echo "=================================================="
docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" .

echo ""
echo "=================================================="
echo " 导出镜像为 .tar.gz 文件"
echo " 输出: ${OUTPUT_FILE}"
echo "=================================================="
# 使用管道将 docker save 的输出直接通过 gzip 压缩，效率更高
docker save "${IMAGE_NAME}:${IMAGE_TAG}" | gzip > "${OUTPUT_FILE}"

echo ""
echo "--------------------------------------------------"
echo " 操作成功!"
echo " 镜像已保存至: ${OUTPUT_FILE}"
echo ""
echo " 您现在可以将此文件传输到另一台机器，并使用以下命令加载它:"
echo "   docker load -i ${OUTPUT_FILE}"
echo "--------------------------------------------------"
