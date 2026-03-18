# warning_system - GitHub CI/CD 到阿里云 ACR 并在 ECS 启动

## 项目结构（仓库根目录）
- `frontend/` Vue3 前端源码
- `backend/` Spring Boot 后端源码（端口 `8001`）
- `Dockerfile` 单镜像构建（前端打包 + 后端静态资源合并）
- `.github/workflows/docker-acr.yml` GitHub Actions 推送 ACR

## GitHub 仓库 Secrets
在仓库 `Settings > Secrets and variables > Actions` 新增：

- `ACR_REGISTRY`
  - 例：`registry.cn-beijing.aliyuncs.com`
- `ACR_IMAGE_NAME`
  - 例：`your-namespace/warning-system`
- `ACR_USERNAME`
  - 阿里云镜像仓库登录用户名
- `ACR_PASSWORD`
  - 阿里云镜像仓库登录密码（或访问凭证）

工作流触发：
- push 到 `main`/`master`
- 或手动 `workflow_dispatch`

## 镜像地址示例
假设：
- `ACR_REGISTRY=registry.cn-beijing.aliyuncs.com`
- `ACR_IMAGE_NAME=your-namespace/warning-system`

则镜像完整地址：
- `registry.cn-beijing.aliyuncs.com/your-namespace/warning-system:latest`

## ECS 服务器部署（Ubuntu 20.04）

### 1. 安装 Docker（若未安装）
```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg lsb-release
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable docker
sudo systemctl start docker
```

### 2. 登录 ACR 并拉取镜像
```bash
sudo docker login --username=<ACR_USERNAME> registry.cn-beijing.aliyuncs.com
sudo docker pull registry.cn-beijing.aliyuncs.com/your-namespace/warning-system:latest
```

### 3. 启动容器（公网访问端口 8001）
```bash
sudo docker rm -f warning-system || true
sudo docker run -d \
  --name warning-system \
  --restart always \
  -p 8001:8001 \
  registry.cn-beijing.aliyuncs.com/your-namespace/warning-system:latest
```

### 4. 放行安全组与防火墙
- 阿里云安全组入方向放行：`8001/tcp`
- 若服务器启用了 UFW：
```bash
sudo ufw allow 8001/tcp
sudo ufw reload
```

### 5. 访问
- `http://<你的公网IP>:8001`

## 常用排查
```bash
sudo docker ps
sudo docker logs -f warning-system
sudo ss -lntp | grep 8001
```
