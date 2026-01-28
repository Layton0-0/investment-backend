/**
 * 공통 유효성 검사 함수
 */

// 유효성 검사 규칙
const ValidationRules = {
    username: {
        pattern: /^[a-zA-Z0-9_]+$/,
        minLength: 4,
        maxLength: 50,
        message: '영문, 숫자, 언더스코어만 사용 가능하며 4-50자여야 합니다.'
    },
    password: {
        minLength: 8,
        maxLength: 100,
        message: '비밀번호는 8자 이상 100자 이하여야 합니다.'
    },
    appKey: {
        minLength: 1,
        message: 'API Key를 입력해주세요.'
    },
    appSecret: {
        minLength: 1,
        message: 'API Secret을 입력해주세요.'
    }
};

/**
 * 사용자 ID 유효성 검사
 */
function validateUsername(username) {
    const rules = ValidationRules.username;
    const trimmed = username.trim();

    if (!trimmed) {
        return { valid: false, message: '사용자 ID를 입력해주세요.' };
    }

    if (trimmed.length < rules.minLength || trimmed.length > rules.maxLength) {
        return { valid: false, message: `사용자 ID는 ${rules.minLength}자 이상 ${rules.maxLength}자 이하여야 합니다.` };
    }

    if (!rules.pattern.test(trimmed)) {
        return { valid: false, message: rules.message };
    }

    return { valid: true, message: '' };
}

/**
 * 비밀번호 유효성 검사
 */
function validatePassword(password, isRequired = true) {
    const rules = ValidationRules.password;

    if (!password) {
        if (isRequired) {
            return { valid: false, message: '비밀번호를 입력해주세요.' };
        }
        return { valid: true, message: '' }; // 선택적 필드는 비어있어도 유효
    }

    if (password.length < rules.minLength) {
        return { valid: false, message: rules.message };
    }

    if (password.length > rules.maxLength) {
        return { valid: false, message: rules.message };
    }

    return { valid: true, message: '' };
}

/**
 * API Key 유효성 검사
 */
function validateAppKey(appKey, isRequired = true) {
    const rules = ValidationRules.appKey;

    if (!appKey) {
        if (isRequired) {
            return { valid: false, message: rules.message };
        }
        return { valid: true, message: '' }; // 선택적 필드는 비어있어도 유효
    }

    if (appKey.trim().length < rules.minLength) {
        return { valid: false, message: rules.message };
    }

    return { valid: true, message: '' };
}

/**
 * API Secret 유효성 검사
 */
function validateAppSecret(appSecret, isRequired = true) {
    const rules = ValidationRules.appSecret;

    if (!appSecret) {
        if (isRequired) {
            return { valid: false, message: rules.message };
        }
        return { valid: true, message: '' }; // 선택적 필드는 비어있어도 유효
    }

    if (appSecret.trim().length < rules.minLength) {
        return { valid: false, message: rules.message };
    }

    return { valid: true, message: '' };
}

/**
 * 증권사 타입 유효성 검사
 */
function validateBrokerType(brokerType, isRequired = true) {
    if (!brokerType) {
        if (isRequired) {
            return { valid: false, message: '사용증권명을 선택해주세요.' };
        }
        return { valid: true, message: '' };
    }

    return { valid: true, message: '' };
}

/**
 * 서버 타입 유효성 검사
 */
function validateServerType(serverType, isRequired = true) {
    if (!serverType) {
        if (isRequired) {
            return { valid: false, message: '서버 타입을 선택해주세요.' };
        }
        return { valid: true, message: '' };
    }

    if (serverType !== '0' && serverType !== '1') {
        return { valid: false, message: '올바른 서버 타입을 선택해주세요.' };
    }

    return { valid: true, message: '' };
}

/**
 * 계좌번호 유효성 검사
 * 형식: 숫자8자리-숫자2자리 (예: 12345678-12)
 */
function validateAccountNumber(accountNo, isRequired = true) {
    if (!accountNo) {
        if (isRequired) {
            return { valid: false, message: '계좌번호를 입력해주세요.' };
        }
        return { valid: true, message: '' };
    }

    const trimmed = accountNo.trim();
    const pattern = /^\d{8}-\d{2}$/;

    if (!pattern.test(trimmed)) {
        return { valid: false, message: '계좌번호 형식이 올바르지 않습니다. 형식: 숫자8자리-숫자2자리 (예: 12345678-12)' };
    }

    return { valid: true, message: '' };
}

/**
 * 입력 필드에 에러 표시
 */
function showFieldError(fieldId, errorMessageId, message) {
    const field = document.getElementById(fieldId);
    const errorElement = document.getElementById(errorMessageId);

    if (field) {
        field.classList.remove('valid');
        field.classList.add('error');
    }

    if (errorElement) {
        errorElement.textContent = message;
        errorElement.classList.add('show');
    }
}

/**
 * 입력 필드에 성공 표시
 */
function showFieldSuccess(fieldId, errorMessageId) {
    const field = document.getElementById(fieldId);
    const errorElement = document.getElementById(errorMessageId);

    if (field) {
        field.classList.remove('error');
        field.classList.add('valid');
    }

    if (errorElement) {
        errorElement.textContent = '';
        errorElement.classList.remove('show');
    }
}

/**
 * 모든 에러 메시지 초기화
 */
function clearAllErrors() {
    document.querySelectorAll('.error-message').forEach(el => {
        el.classList.remove('show');
        el.textContent = '';
    });

    document.querySelectorAll('input, select, textarea').forEach(el => {
        el.classList.remove('error', 'valid');
    });
}

/**
 * 실시간 유효성 검사 (입력 중)
 */
function setupRealTimeValidation(fieldId, validator, errorMessageId) {
    const field = document.getElementById(fieldId);
    if (!field) return;

    field.addEventListener('blur', () => {
        const value = field.value;
        const result = validator(value);

        if (result.valid) {
            showFieldSuccess(fieldId, errorMessageId);
        } else {
            showFieldError(fieldId, errorMessageId, result.message);
        }
    });

    field.addEventListener('input', () => {
        // 입력 중에는 에러 클래스만 제거 (성공 표시는 blur에서)
        field.classList.remove('error');
        const errorElement = document.getElementById(errorMessageId);
        if (errorElement) {
            errorElement.classList.remove('show');
        }
    });
}
