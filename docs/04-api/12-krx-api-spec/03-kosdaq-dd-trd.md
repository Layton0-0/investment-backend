# KOSDAQ 시리즈 일별시세정보 (kosdaq_dd_trd)

## 1. 개요

- **API 명칭**: KOSDAQ 시리즈 일별시세정보
- **설명**: KOSDAQ 시리즈 지수의 시세정보 제공 ('10년01월04일 데이터부터 제공)
- **Server endpoint**: `https://data-dbg.krx.co.kr/svc/apis/idx/kosdaq_dd_trd`
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
| IDX_CLSS | string | 계열구분 |
| IDX_NM | string | 지수명 |
| CLSPRC_IDX | string | 종가 |
| CMPPREVDD_IDX | string | 대비 |
| FLUC_RT | string | 등락률 |
| OPNPRC_IDX | string | 시가 |
| HGPRC_IDX | string | 고가 |
| LWPRC_IDX | string | 저가 |
| ACC_TRDVOL | string | 거래량 |
| ACC_TRDVAL | string | 거래대금 |
| MKTCAP | string | 상장시가총액 |

## 4. Request Sample

```json
{"basDd":"yyyyMMdd"}
```

쿼리 파라미터: `?basDd=yyyyMMdd`

## 5. Response Sample

```json
{
  "OutBlock_1": [
    {
      "BAS_DD": "...",
      "IDX_CLSS": "...",
      "IDX_NM": "...",
      "CLSPRC_IDX": "...",
      "CMPPREVDD_IDX": "...",
      "FLUC_RT": "...",
      "OPNPRC_IDX": "...",
      "HGPRC_IDX": "...",
      "LWPRC_IDX": "...",
      "ACC_TRDVOL": "...",
      "ACC_TRDVAL": "...",
      "MKTCAP": "..."
    }
  ]
}
```

## 6. 참고

- 메인 명세: [../12-krx-api-spec.md](../12-krx-api-spec.md)
- 지수·벤치마크·업종 분석 시 활용 가능.
