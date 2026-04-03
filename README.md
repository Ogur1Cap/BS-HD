# DeltaForce 后端（Spring Boot）

基于 Spring Boot 3 + MySQL 8 + JWT/RefreshToken 的后端实现，用于对接前端项目 `M:\Vue\DeltaForce\DeltaForce`。

## 1. 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8+

## 2. 初始化数据库

```sql
CREATE DATABASE deltaforce DEFAULT CHARACTER SET utf8mb4;
```

默认连接信息在 `src/main/resources/application-dev.yml`：

- 用户名：`root`
- 密码：`123456`
- 库名：`deltaforce`

可按实际环境修改。

如果本机 MySQL 不是 `root/root`，可以不改文件，直接在命令行临时覆盖：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/deltaforce?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"
$env:DB_USERNAME="你的MySQL用户名"
$env:DB_PASSWORD="你的MySQL密码"
mvn spring-boot:run
```

## 3. 启动项目

```bash
mvn spring-boot:run
```

启动后：

- API 地址：`http://localhost:8080`
- Swagger：`http://localhost:8080/swagger-ui/index.html`

## 4. 前端联调配置

前端 `.env` 建议配置：

```env
VITE_API_MODE=real
VITE_API_BASE_URL=http://localhost:8080
```

## 5. 已实现接口

### 鉴权

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

### 用户资料

- `GET /profile`
- `PUT /profile`

### 订单

- `GET /orders`
- `POST /orders`
- `GET /orders/{orderId}`
- `POST /orders/{orderId}/cancel`
- `POST /orders/{orderId}/reschedule`
- `POST /orders/{orderId}/refund`

### 通知

- `GET /notifications`
- `POST /notifications/mark-read`
- `POST /notifications/mark-all-read`
- `DELETE /notifications/{notificationId}`

## 6. 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 7. 运行测试

```bash
mvn test
```

测试使用 H2 内存数据库，不依赖本地 MySQL。
