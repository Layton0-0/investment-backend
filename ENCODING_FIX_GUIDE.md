# 인코딩 문제 해결 가이드

## 문제 상황

프로젝트의 Java 소스 파일에서 한글 주석이 깨져서 표시되는 문제가 발생했습니다.

## 원인

1. **Gradle 빌드 설정 부족**: `build.gradle`에 Java 컴파일 인코딩 설정이 없었음
2. **IDE 설정 부족**: `.editorconfig` 파일이 없어 IDE 인코딩 설정이 일관되지 않음
3. **파일 저장 인코딩**: 일부 파일이 UTF-8이 아닌 다른 인코딩으로 저장됨

## 해결 방법

### 1. build.gradle 인코딩 설정 추가

```gradle
// Java 컴파일 인코딩 설정
tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
}

// JavaDoc 인코딩 설정
tasks.withType(Javadoc) {
    options.encoding = 'UTF-8'
}

// 리소스 처리 인코딩 설정
processResources {
    encoding = 'UTF-8'
}
```

### 2. .editorconfig 파일 생성

프로젝트 루트에 `.editorconfig` 파일을 생성하여 모든 파일의 인코딩을 UTF-8로 통일했습니다.

### 3. gradle.properties 확인

`gradle.properties`에 다음 설정이 있는지 확인:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

### 4. IDE 설정 확인

#### IntelliJ IDEA / Cursor
1. File → Settings → Editor → File Encodings
2. Global Encoding: UTF-8
3. Project Encoding: UTF-8
4. Default encoding for properties files: UTF-8
5. Transparent native-to-ascii conversion 체크

#### VS Code
1. Settings → Files: Encoding → UTF-8
2. 또는 `.vscode/settings.json`에 추가:
```json
{
  "files.encoding": "utf8",
  "files.autoGuessEncoding": true
}
```

## 수정된 파일 목록

다음 파일들의 한글 주석을 UTF-8로 수정했습니다:

1. `src/main/java/com/investment/InvestmentApplication.java`
2. `src/main/java/com/investment/marketdata/config/MarketDataProperties.java`
3. `src/main/java/com/investment/marketdata/util/KiwoomCodeConverter.java`
4. `src/main/java/com/investment/marketdata/client/impl/KiwoomMarketDataClient.java`

## 추가 확인 사항

### 다른 파일도 확인 필요

다음 명령으로 한글이 깨진 파일을 찾을 수 있습니다:

```bash
# Windows PowerShell
Get-ChildItem -Recurse -Include *.java | Select-String -Pattern "[^\x00-\x7F]" | Select-Object Path, LineNumber, Line
```

### 빌드 후 확인

```bash
# Gradle 빌드 실행
./gradlew clean build

# 컴파일된 클래스 파일의 인코딩 확인
javap -verbose build/classes/java/main/com/investment/InvestmentApplication.class
```

## 예방 방법

1. **IDE 설정 통일**: 모든 개발자가 동일한 인코딩 설정 사용
2. **.editorconfig 활용**: 프로젝트 루트에 `.editorconfig` 파일로 인코딩 강제
3. **Git 설정**: `.gitattributes` 파일에 인코딩 설정 추가
4. **빌드 스크립트**: `build.gradle`에 인코딩 설정 명시

## .gitattributes 추가 (선택사항)

프로젝트 루트에 `.gitattributes` 파일을 추가하여 Git에서도 인코딩을 관리할 수 있습니다:

```
*.java text eol=lf charset=utf-8
*.properties text eol=lf charset=utf-8
*.yml text eol=lf charset=utf-8
*.yaml text eol=lf charset=utf-8
*.md text eol=lf charset=utf-8
```

## 참고

- [Gradle 인코딩 설정](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_system_properties)
- [EditorConfig](https://editorconfig.org/)
- [Java 인코딩 문제 해결](https://docs.oracle.com/javase/tutorial/i18n/text/index.html)
