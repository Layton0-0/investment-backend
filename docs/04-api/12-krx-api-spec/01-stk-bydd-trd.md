# 유가증권 일별매매정보 (stk_bydd_trd)

## 1. 개요

- **API 명칭**: 유가증권 일별매매정보
- **설명**: 유가증권시장에 상장되어 있는 주권의 매매정보 제공 ('10년01월04일 데이터부터 제공)
- **Server endpoint**: `https://data-dbg.krx.co.kr/svc/apis/sto/stk_bydd_trd`
- **인증**: Request 헤더 `AUTH_KEY`에 인증키 전달

## 2. Request

### 2.1 InBlock_1

| Name | Type | Description |
|------|------|--------------|
| basDd | string | 기준일자 (yyyyMMdd) |

## 3. Response

### 3.1 OutBlock_1

| Name | Type | Description |
|------|------|--------------|
| BAS_DD | string | 기준일자 |
| ISU_CD | string | 종목코드 |
| ISU_NM | string | 종목명 |
| MKT_NM | string | 시장구분 |
| SECT_TP_NM | string | 소속부 |
| TDD_CLSPRC | string | 종가 |
| CMPPREVDD_PRC | string | 대비 |
| FLUC_RT | string | 등락률 |
| TDD_OPNPRC | string | 시가 |
| TDD_HGPRC | string | 고가 |
| TDD_LWPRC | string | 저가 |
| ACC_TRDVOL | string | 거래량 |
| ACC_TRDVAL | string | 거래대금 |
| MKTCAP | string | 시가총액 |
| LIST_SHRS | string | 상장주식수 |

## 4. Request Sample

```json
{"basDd":"yyyyMMdd"}
```

쿼리 파라미터로 전달 시: `?basDd=yyyyMMdd`

## 5. Response Sample

```json
{
  "OutBlock_1": [
    {
      "BAS_DD": "...",
      "ISU_CD": "...",
      "ISU_NM": "...",
      "MKT_NM": "...",
      "SECT_TP_NM": "...",
      "TDD_CLSPRC": "...",
      "CMPPREVDD_PRC": "...",
      "FLUC_RT": "...",
      "TDD_OPNPRC": "...",
      "TDD_HGPRC": "...",
      "TDD_LWPRC": "...",
      "ACC_TRDVOL": "...",
      "ACC_TRDVAL": "...",
      "MKTCAP": "...",
      "LIST_SHRS": "..."
    }
  ]
}
```

## 6. 구현 참고

- **KrxApiClient**: `fetchDailyStockKospi(LocalDate basDt)`에서 GET으로 호출, `KrxDailyStockResponseDto`로 수신.
- **KrxCollectionService**: OutBlock_1 필드명(ISU_CD, TDD_OPNPRC, TDD_HGPRC, TDD_LWPRC, TDD_CLSPRC, ACC_TRDVOL, ACC_TRDVAL)으로 매핑 후 TB_DAILY_STOCK 저장. 코스닥(ksq_bydd_trd) 결과와 합쳐 저장.
- 메인 명세: [../12-krx-api-spec.md](../12-krx-api-spec.md)
