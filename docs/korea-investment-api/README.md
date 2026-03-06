# 한국투자증권 API 원본 명세 (엑셀)

이 폴더에는 한국투자증권 Open API 공식 명세 엑셀 파일이 있으며, 백엔드 문서화 시 참조합니다.

## 파일 목록

| 파일명 | API ID | 설명 | 문서화 위치 |
|--------|--------|------|-------------|
| `Hashkey.xlsx` | Hashkey | 요청 무결성 검증용 Hashkey 발급 (POST /uapi/hashkey) | [09-korea-investment-api-guide.md](../04-api/09-korea-investment-api-guide.md) § Hashkey API |
| `실시간 (웹소켓) 접속키 발급[실시간-000].xlsx` | 실시간-000 | 웹소켓용 approval_key 발급 (POST /oauth2/Approval) | [09-korea-investment-api-guide.md](../04-api/09-korea-investment-api-guide.md) § WebSocket approval_key 발급 |

## 사용

- **문서화**: 위 가이드에서 Request/Response 스펙·예시를 엑셀 명세 기준으로 유지합니다.
- **MCP**: Excel Reader MCP로 시트 목록·내용을 읽어 명세를 확인할 수 있습니다.
- **보안**: 엑셀 파일에는 실제 appkey/appsecret을 기입하지 않으며, 예시만 포함합니다.
