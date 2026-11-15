# Aptasks Backend - Kotlin/Spring Boot

API REST de autenticação desenvolvida em Kotlin com Spring Boot.

## 🚀 Tecnologias

- **Kotlin** 1.9.20
- **Spring Boot** 3.2.0
- **Supabase Auth** para autenticação
- **OkHttp** para HTTP Client
- **Gradle** para build

## 📋 Pré-requisitos

- JDK 17 ou superior
- Gradle 8.x (ou use o wrapper incluído)

## ⚙️ Configuração

1. Instale/inicie o PostgreSQL e crie o banco `aptasks`.

2. Configure as variáveis de ambiente no PowerShell antes de executar o backend:
```
$env:DB_URL="jdbc:postgresql://localhost:5432/aptasks"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="sua_senha_do_postgres"
```

`DB_URL` pode apontar para qualquer servidor PostgreSQL acessível, local ou remoto. O backend usa `localhost:5432/aptasks`, usuário `postgres` e senha vazia como valores padrão. Para autenticação Supabase, configure também `SUPABASE_API_URL` e `SUPABASE_KEY`.

## 🏃 Como executar

### Windows (PowerShell):
```powershell
.\gradlew.bat bootRun
```

### Linux/Mac:
```bash
./gradlew bootRun
```

O servidor estará rodando em `http://localhost:3000`

## 🗃️ Diagramas do banco

Consulte [docs/diagramas-banco.md](docs/diagramas-banco.md) para ver os diagramas ER e relacional. Eles foram reconstruídos do código e devem ser conferidos com o schema real do PostgreSQL.

Para ativar o histórico semanal de atribuições, execute `src/main/resources/db/migration/V4__task_rotation_history.sql` no banco PostgreSQL depois das migrações V2 e V3. As migrações são scripts SQL e não são aplicadas automaticamente pelo `bootRun`.

## 📡 Endpoint

### Autenticação

- **POST /auth/login** - Login de usuário
  
  **Request:**
  ```json
  {
    "email": "usuario@exemplo.com",
    "password": "senha123"
  }
  ```
  
  **Response (Sucesso - 200 OK):**
  ```json
  {
    "success": true,
    "message": "Login realizado com sucesso",
    "data": {
      "access_token": "eyJhbGc...",
      "token_type": "bearer",
      "expires_in": 3600,
      "refresh_token": "...",
      "user": {
        "id": "uuid-do-usuario",
        "email": "usuario@exemplo.com",
        "role": "authenticated",
        "created_at": "2025-12-18T..."
      }
    }
  }
  ```
  
  **Response (Erro - 400/401):**
  ```json
  {
    "success": false,
    "message": "Falha no login",
    "error": "Credenciais inválidas"
  }
  ```

## 🔧 Build

### Gerar JAR:
```bash
.\gradlew.bat build
```

O JAR será gerado em `build/libs/`

### Executar o JAR:
```bash
java -jar build/libs/aptasks-backend-1.0.0.jar
```

## 📦 Deploy

### Configurar variáveis de ambiente no servidor:
```
DB_URL=jdbc:postgresql://host:5432/aptasks
DB_USERNAME=...
DB_PASSWORD=...
SUPABASE_API_URL=...
SUPABASE_KEY=...
```

### Executar em produção:
```bash
java -jar -Dspring.profiles.active=prod aptasks-backend-1.0.0.jar
```

## 🛠️ Estrutura do Projeto

```
src/main/kotlin/com/aptasks/
├── AptasksApplication.kt          # Main class
├── config/
│   ├── CorsConfig.kt              # Configuração CORS
│   └── SupabaseConfig.kt          # Configuração Supabase
├── controller/
│   └── AuthController.kt          # Endpoint de autenticação
├── dto/
│   ├── ApiResponse.kt             # Response padrão
│   ├── LoginRequest.kt            # Request de login
│   └── LoginResponse.kt           # Response de login
└── service/
    └── AuthService.kt             # Lógica de autenticação
```

## 🔐 CORS

O CORS está configurado para aceitar requisições de:
- `http://localhost:3000`
- `http://localhost:3001`
- `https://seu-frontend.onrender.com`

Para adicionar mais origens, edite `config/CorsConfig.kt`

## 🧪 Testando a API

```powershell
# Login
curl -X POST http://localhost:3000/auth/login -H "Content-Type: application/json" -d '{\"email\":\"seu@email.com\",\"password\":\"suasenha\"}'
```

