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

1. Copie o arquivo `.env.example` para `.env`:
```bash
cp .env.example .env
```

2. Configure as variáveis de ambiente no `.env`:
```
SUPABASE_URL=sua_url_do_supabase
SUPABASE_SERVICE_KEY=sua_chave_de_servico
```

3. Ou configure diretamente no `application.properties`

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
SUPABASE_URL=...
SUPABASE_SERVICE_KEY=...
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

