# Ecommerce Project — Setup & Build Log

A running record of everything set up in this project, in the order it was built, with all code. Use this as your study reference.

---

## 1. Project baseline

Spring Boot 4.1.1, Java 21, Maven. Starting dependencies in `pom.xml`:

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-flyway`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `spring-boot-starter-webmvc`
- `flyway-database-postgresql`
- `org.postgresql:postgresql` (runtime)
- `org.projectlombok:lombok` (optional)
- corresponding `-test` starters

Database: PostgreSQL (external, hosted via Dokploy). File storage: MinIO (external, hosted via Dokploy).

---

## 2. Environment configuration

### `.env` (project root — **never commit this**, add to `.gitignore`)

```
DB_HOST=162.35.106.60
DB_PORT=5438
DB_NAME=postgres
DB_USER=postgres
DB_PASSWORD=a6sd3deshcelarny

MINIO_ENDPOINT=http://ecommercetest-minio-937275-162-35-106-60.sslip.io
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=tqfgctaq4htixjk1
MINIO_BUCKET_NAME=category
MINIO_PUBLIC_URL=http://ecommercetest-minio-937275-162-35-106-60.sslip.io

JWT_SECRET=Base64EncodedSecretAtLeast256BitsLongChangeThis1234567890==
JWT_ACCESS_EXPIRY_MS=900000
JWT_REFRESH_EXPIRY_MS=604800000
```

Generate a real JWT secret with `openssl rand -base64 32` instead of the placeholder above.

### `.gitignore` — add

```
.env
```

### How `.env` gets loaded

We first tried `me.paulschwarz:spring-dotenv` as a dependency, but it registers via the legacy `SpringApplicationRunListener` mechanism (`META-INF/spring.factories`) which did not work reliably on Spring Boot 4.1.1 — placeholders like `${DB_HOST}` stayed unresolved.

**Final working approach — no extra dependency needed.** Since `.env` (`KEY=VALUE` per line) is valid `.properties` syntax, Spring Boot's built-in config-import feature loads it directly:

```properties
spring.config.import=optional:file:.env[.properties]
```

`optional:` means the app won't fail to start if `.env` is missing (e.g. in production where real env vars are injected instead).

### `src/main/resources/application.properties` (final)

```properties
spring.config.import=optional:file:.env[.properties]
spring.application.name=ecommerce

spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

minio.endpoint=${MINIO_ENDPOINT}
minio.access-key=${MINIO_ACCESS_KEY}
minio.secret-key=${MINIO_SECRET_KEY}
minio.bucket-name=${MINIO_BUCKET_NAME}
minio.public-url=${MINIO_PUBLIC_URL}

jwt.secret=${JWT_SECRET}
jwt.access-expiry-ms=${JWT_ACCESS_EXPIRY_MS}
jwt.refresh-expiry-ms=${JWT_REFRESH_EXPIRY_MS}

spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
```

**Gotcha we hit:** trailing whitespace on property lines gets included as part of the value (Java `.properties` parsing strips leading whitespace but not trailing). This caused `spring.datasource.username` to resolve to `"postgres" + lots of spaces`, which Postgres rejected as a bad username. Keep lines clean — no trailing spaces. Enable "trim trailing whitespace on save" in your editor.

---

## 3. JDK setup

Multiple JDKs were present on the machine (JDK 26 default, JDK 21 needed for this project — `java.version=21` in `pom.xml`).

`mvnw` reads the `JAVA_HOME` environment variable specifically (not just `PATH`), so it must be set explicitly every new terminal session:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.12"
```

---

## 4. Running the app

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21.0.12"
.\mvnw spring-boot:run "-Dspring-boot.run.jvmArguments=-Duser.timezone=UTC"
```

**Gotcha we hit — timezone:** the Postgres JDBC driver sends the JVM's default timezone ID during connection handshake. On Windows, Java resolves India's zone to the legacy alias `Asia/Calcutta`, which some minimal Postgres server builds don't recognize (only canonical IANA names like `Asia/Kolkata`/`UTC`). Fix: force `-Duser.timezone=UTC` as a JVM argument.

App runs at `http://localhost:8080`.

---

## 5. Flyway migrations

Location: `src/main/resources/db/migration/`

Naming rule: `V<version>__<description>.sql` — capital `V`, version number, **double underscore**, snake_case description, `.sql` extension. Flyway auto-runs any new migration file on startup; folder must not be empty once Flyway is on the classpath.

### `V1__create_users_and_refresh_tokens.sql`

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```

### `V2__create_categories_and_subcategories.sql`

```sql
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE subcategories (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(category_id, name)
);
```

### `V3__create_products.sql`

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    subcategory_id BIGINT NOT NULL REFERENCES subcategories(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    price NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
```

---

## 6. Swagger / OpenAPI

### `pom.xml`

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.5</version>
</dependency>
```

Note: Spring Boot 4.1.1 is very new (Spring Framework 7); if this version mismatches, check [mvnrepository.com](https://mvnrepository.com/artifact/org.springdoc/springdoc-openapi-starter-webmvc-ui) for a newer release.

Access once running:
- UI: `http://localhost:8080/swagger-ui/index.html`
- Raw spec: `http://localhost:8080/v3/api-docs`

These paths must be explicitly permitted in Spring Security (see `SecurityConfig` below), otherwise they're blocked by default.

---

## 7. Auth module (signup / login / JWT access + refresh tokens)

Package layout used:
- `model` → JPA entities
- `view` → request/response DTOs
- `repository` → Spring Data JPA repositories
- `service` → business logic
- `security` → JWT filter
- `controller` → REST endpoints
- `config` → security wiring

### `pom.xml` — JWT library

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

### `model/User.java`

```java
package com.ram.ecommerce.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### `model/RefreshToken.java`

```java
package com.ram.ecommerce.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true)
    private String token;

    private LocalDateTime expiryDate;

    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### `view/SignupRequest.java`

```java
package com.ram.ecommerce.view;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password
) {}
```

### `view/LoginRequest.java`

```java
package com.ram.ecommerce.view;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
```

### `view/AuthResponse.java`

```java
package com.ram.ecommerce.view;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}
```

### `view/RefreshTokenRequest.java`

```java
package com.ram.ecommerce.view;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank String refreshToken
) {}
```

### `repository/UserRepository.java`

```java
package com.ram.ecommerce.repository;

import com.ram.ecommerce.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### `repository/RefreshTokenRepository.java`

```java
package com.ram.ecommerce.repository;

import com.ram.ecommerce.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUserId(Long userId);
}
```

### `service/JwtService.java`

```java
package com.ram.ecommerce.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpiryMs;
    private final long refreshExpiryMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiry-ms}") long accessExpiryMs,
            @Value("${jwt.refresh-expiry-ms}") long refreshExpiryMs
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessExpiryMs = accessExpiryMs;
        this.refreshExpiryMs = refreshExpiryMs;
    }

    public String generateAccessToken(String email) {
        return buildToken(email, accessExpiryMs);
    }

    public String generateRefreshToken(String email) {
        return buildToken(email, refreshExpiryMs);
    }

    private String buildToken(String email, long expiryMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### `service/AuthService.java`

```java
package com.ram.ecommerce.service;

import com.ram.ecommerce.model.RefreshToken;
import com.ram.ecommerce.model.User;
import com.ram.ecommerce.repository.RefreshTokenRepository;
import com.ram.ecommerce.repository.UserRepository;
import com.ram.ecommerce.view.AuthResponse;
import com.ram.ecommerce.view.LoginRequest;
import com.ram.ecommerce.view.SignupRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (stored.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("Refresh token expired");
        }

        String email = stored.getUser().getEmail();
        String newAccessToken = jwtService.generateAccessToken(email);

        return new AuthResponse(newAccessToken, refreshToken);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshTokenValue = jwtService.generateRefreshToken(user.getEmail());

        refreshTokenRepository.deleteByUserId(user.getId());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, refreshTokenValue);
    }
}
```

> **Gotcha we hit:** forgot the `@Service` annotation on this class initially — it compiled fine but Spring couldn't find it as a bean (`No qualifying bean of type 'AuthService'`). Whenever you see "No qualifying bean" / "required a bean of type X that could not be found", check first whether the class is missing `@Service`/`@Component`/`@Repository`/`@Controller`.

### `controller/AuthController.java`

```java
package com.ram.ecommerce.controller;

import com.ram.ecommerce.service.AuthService;
import com.ram.ecommerce.view.AuthResponse;
import com.ram.ecommerce.view.LoginRequest;
import com.ram.ecommerce.view.RefreshTokenRequest;
import com.ram.ecommerce.view.SignupRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }
}
```

### `security/JwtAuthFilter.java`

```java
package com.ram.ecommerce.security;

import com.ram.ecommerce.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtService.isTokenValid(token)) {
                String email = jwtService.extractEmail(token);

                var authentication = new UsernamePasswordAuthenticationToken(
                        email, null, List.of()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### `config/SecurityConfig.java` (final)

```java
package com.ram.ecommerce.config;

import com.ram.ecommerce.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

Test:

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Roy","email":"roy@example.com","password":"password123"}'
```

---

## 8. Category / SubCategory module (with MinIO image upload)

### `pom.xml` — MinIO client

```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.11</version>
</dependency>
```

### `config/MinioConfig.java`

```java
package com.ram.ecommerce.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey
    ) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

### `service/MinioService.java`

```java
package com.ram.ecommerce.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class MinioService {

    private final MinioClient minioClient;
    private final String bucketName;
    private final String publicUrl;

    public MinioService(
            MinioClient minioClient,
            @Value("${minio.bucket-name}") String bucketName,
            @Value("${minio.public-url}") String publicUrl
    ) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        this.publicUrl = publicUrl;
    }

    @PostConstruct
    public void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    public String uploadFile(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            return publicUrl + "/" + bucketName + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }
}
```

Note: `ensureBucketExists()` auto-creates the bucket at startup, but the bucket also needs a public-read access policy set in the MinIO console if you want the returned `imageUrl` directly browsable (otherwise uploads succeed but viewing the raw URL returns 403).

### `model/Category.java`

```java
package com.ram.ecommerce.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Getter
@Setter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String imageUrl;

    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### `model/SubCategory.java`

```java
package com.ram.ecommerce.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "subcategories")
@Getter
@Setter
public class SubCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    private String imageUrl;

    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### `view/CategoryResponse.java`

```java
package com.ram.ecommerce.view;

public record CategoryResponse(Long id, String name, String imageUrl) {}
```

### `view/SubCategoryResponse.java`

```java
package com.ram.ecommerce.view;

public record SubCategoryResponse(Long id, String name, String imageUrl, Long categoryId) {}
```

### `repository/CategoryRepository.java`

```java
package com.ram.ecommerce.repository;

import com.ram.ecommerce.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);
}
```

### `repository/SubCategoryRepository.java`

```java
package com.ram.ecommerce.repository;

import com.ram.ecommerce.model.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {
    List<SubCategory> findByCategoryId(Long categoryId);
}
```

### `service/CategoryService.java`

```java
package com.ram.ecommerce.service;

import com.ram.ecommerce.model.Category;
import com.ram.ecommerce.repository.CategoryRepository;
import com.ram.ecommerce.view.CategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MinioService minioService;

    public CategoryService(CategoryRepository categoryRepository, MinioService minioService) {
        this.categoryRepository = categoryRepository;
        this.minioService = minioService;
    }

    public CategoryResponse create(String name, MultipartFile image) {
        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("Category already exists");
        }

        String imageUrl = minioService.uploadFile(image);

        Category category = new Category();
        category.setName(name);
        category.setImageUrl(imageUrl);
        categoryRepository.save(category);

        return toResponse(category);
    }

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getImageUrl());
    }
}
```

### `service/SubCategoryService.java`

```java
package com.ram.ecommerce.service;

import com.ram.ecommerce.model.Category;
import com.ram.ecommerce.model.SubCategory;
import com.ram.ecommerce.repository.CategoryRepository;
import com.ram.ecommerce.repository.SubCategoryRepository;
import com.ram.ecommerce.view.SubCategoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final MinioService minioService;

    public SubCategoryService(
            SubCategoryRepository subCategoryRepository,
            CategoryRepository categoryRepository,
            MinioService minioService
    ) {
        this.subCategoryRepository = subCategoryRepository;
        this.categoryRepository = categoryRepository;
        this.minioService = minioService;
    }

    public SubCategoryResponse create(Long categoryId, String name, MultipartFile image) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        String imageUrl = minioService.uploadFile(image);

        SubCategory subCategory = new SubCategory();
        subCategory.setCategory(category);
        subCategory.setName(name);
        subCategory.setImageUrl(imageUrl);
        subCategoryRepository.save(subCategory);

        return toResponse(subCategory);
    }

    public List<SubCategoryResponse> getByCategory(Long categoryId) {
        return subCategoryRepository.findByCategoryId(categoryId).stream().map(this::toResponse).toList();
    }

    private SubCategoryResponse toResponse(SubCategory subCategory) {
        return new SubCategoryResponse(
                subCategory.getId(),
                subCategory.getName(),
                subCategory.getImageUrl(),
                subCategory.getCategory().getId()
        );
    }
}
```

### `controller/CategoryController.java`

```java
package com.ram.ecommerce.controller;

import com.ram.ecommerce.service.CategoryService;
import com.ram.ecommerce.view.CategoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<CategoryResponse> create(
            @RequestParam("name") String name,
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(categoryService.create(name, image));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }
}
```

### `controller/SubCategoryController.java`

```java
package com.ram.ecommerce.controller;

import com.ram.ecommerce.service.SubCategoryService;
import com.ram.ecommerce.view.SubCategoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/categories/{categoryId}/subcategories")
public class SubCategoryController {

    private final SubCategoryService subCategoryService;

    public SubCategoryController(SubCategoryService subCategoryService) {
        this.subCategoryService = subCategoryService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<SubCategoryResponse> create(
            @PathVariable Long categoryId,
            @RequestParam("name") String name,
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(subCategoryService.create(categoryId, name, image));
    }

    @GetMapping
    public ResponseEntity<List<SubCategoryResponse>> getAll(@PathVariable Long categoryId) {
        return ResponseEntity.ok(subCategoryService.getByCategory(categoryId));
    }
}
```

No `SecurityConfig` change needed — `anyRequest().authenticated()` already covers these, so a valid Bearer token is required automatically.

---

## 9. Product module (under SubCategory)

### `model/Product.java`

```java
package com.ram.ecommerce.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "subcategory_id", nullable = false)
    private SubCategory subCategory;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String imageUrl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private LocalDateTime createdAt = LocalDateTime.now();
}
```

`BigDecimal` used for price deliberately — never `double`/`float` for money, they introduce rounding errors.

### `view/ProductResponse.java`

```java
package com.ram.ecommerce.view;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String title,
        String description,
        String imageUrl,
        BigDecimal price,
        Long subCategoryId
) {}
```

### `repository/ProductRepository.java`

```java
package com.ram.ecommerce.repository;

import com.ram.ecommerce.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySubCategoryId(Long subCategoryId);
}
```

### `service/ProductService.java`

```java
package com.ram.ecommerce.service;

import com.ram.ecommerce.model.Product;
import com.ram.ecommerce.model.SubCategory;
import com.ram.ecommerce.repository.ProductRepository;
import com.ram.ecommerce.repository.SubCategoryRepository;
import com.ram.ecommerce.view.ProductResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final MinioService minioService;

    public ProductService(
            ProductRepository productRepository,
            SubCategoryRepository subCategoryRepository,
            MinioService minioService
    ) {
        this.productRepository = productRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.minioService = minioService;
    }

    public ProductResponse create(
            Long subCategoryId,
            String name,
            String title,
            String description,
            BigDecimal price,
            MultipartFile image
    ) {
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("Subcategory not found"));

        String imageUrl = minioService.uploadFile(image);

        Product product = new Product();
        product.setSubCategory(subCategory);
        product.setName(name);
        product.setTitle(title);
        product.setDescription(description);
        product.setPrice(price);
        product.setImageUrl(imageUrl);
        productRepository.save(product);

        return toResponse(product);
    }

    public List<ProductResponse> getBySubCategory(Long subCategoryId) {
        return productRepository.findBySubCategoryId(subCategoryId).stream().map(this::toResponse).toList();
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getTitle(),
                product.getDescription(),
                product.getImageUrl(),
                product.getPrice(),
                product.getSubCategory().getId()
        );
    }
}
```

### `controller/ProductController.java`

```java
package com.ram.ecommerce.controller;

import com.ram.ecommerce.service.ProductService;
import com.ram.ecommerce.view.ProductResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/subcategories/{subCategoryId}/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ProductResponse> create(
            @PathVariable Long subCategoryId,
            @RequestParam("name") String name,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("price") BigDecimal price,
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(productService.create(subCategoryId, name, title, description, price, image));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll(@PathVariable Long subCategoryId) {
        return ResponseEntity.ok(productService.getBySubCategory(subCategoryId));
    }
}
```

No `SecurityConfig` change needed — covered by `anyRequest().authenticated()`.

---

## 10. Entity relationship chain

```
User  ──┐
        └── RefreshToken (1:1 active token per user, replaced on each login)

Category (1) ── SubCategory (many) ── Product (many)
```

## 11. Full package structure (as of this log)

```
com.ram.ecommerce
├── EcommerceApplication.java
├── config
│   ├── SecurityConfig.java
│   └── MinioConfig.java
├── model
│   ├── User.java
│   ├── RefreshToken.java
│   ├── Category.java
│   ├── SubCategory.java
│   └── Product.java
├── view
│   ├── SignupRequest.java
│   ├── LoginRequest.java
│   ├── AuthResponse.java
│   ├── RefreshTokenRequest.java
│   ├── CategoryResponse.java
│   ├── SubCategoryResponse.java
│   └── ProductResponse.java
├── repository
│   ├── UserRepository.java
│   ├── RefreshTokenRepository.java
│   ├── CategoryRepository.java
│   ├── SubCategoryRepository.java
│   └── ProductRepository.java
├── service
│   ├── JwtService.java
│   ├── AuthService.java
│   ├── MinioService.java
│   ├── CategoryService.java
│   ├── SubCategoryService.java
│   └── ProductService.java
├── security
│   └── JwtAuthFilter.java
└── controller
    ├── AuthController.java
    ├── CategoryController.java
    ├── SubCategoryController.java
    └── ProductController.java
```

## 12. Testing flow end-to-end

1. `POST /api/auth/signup` → get `accessToken` + `refreshToken`
2. Use `Authorization: Bearer <accessToken>` header for everything below
3. `POST /api/categories` (multipart: `name`, `image`) → category with `imageUrl`
4. `POST /api/categories/{categoryId}/subcategories` (multipart: `name`, `image`)
5. `POST /api/subcategories/{subCategoryId}/products` (multipart: `name`, `title`, `description`, `price`, `image`)
6. `POST /api/auth/refresh` with `refreshToken` once the access token expires (15 min default)
