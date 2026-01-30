package com.investment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * .env 파일을 읽어 시스템 프로퍼티로 주입.
 * IntelliJ 등에서 .env를 자동 로드하지 않을 때, 앱 시작 전 한 번만 실행하여
 * application.yml의 ${VAR:default}가 .env 값을 사용할 수 있게 한다.
 * 이미 설정된 환경 변수/시스템 프로퍼티는 덮어쓰지 않는다.
 */
public final class DotEnvLoader {

    private static final Logger log = LoggerFactory.getLogger(DotEnvLoader.class);

    private DotEnvLoader() {
    }

    /**
     * user.dir 기준 .env 파일을 찾아 KEY=VALUE를 시스템 프로퍼티로 설정.
     * (이미 설정된 키는 건너뜀)
     */
    public static void loadIfPresent() {
        Path cwd = Paths.get(System.getProperty("user.dir", "."));
        Path envFile = cwd.resolve(".env");
        if (!Files.isRegularFile(envFile)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
            int set = 0;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (key.isEmpty()) {
                    continue;
                }
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                    set++;
                }
            }
            if (set > 0) {
                log.debug("DotEnvLoader: loaded {} variables from .env", set);
            }
        } catch (IOException e) {
            log.debug("DotEnvLoader: could not read .env: {}", e.getMessage());
        }
    }
}
