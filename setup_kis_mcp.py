# -*- coding: utf-8 -*-
"""한국투자증권 MCP 환경 설정 스크립트"""
import os
import sys

# Windows 콘솔 인코딩 설정
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

def setup_folders():
    """필요한 폴더 생성"""
    home = os.path.expanduser('~')
    
    # KIS/config 폴더 생성
    kis_config = os.path.join(home, 'KIS', 'config')
    os.makedirs(kis_config, exist_ok=True)
    print(f"[OK] KIS config folder created: {kis_config}")
    
    # 자동매매 폴더 생성 (UTF-8 인코딩 명시)
    auto_trading = os.path.join(home, '자동매매')
    try:
        os.makedirs(auto_trading, exist_ok=True)
        print(f"[OK] 자동매매 folder created: {auto_trading}")
    except Exception as e:
        print(f"[ERROR] 자동매매 folder creation failed: {e}")
        # 영문 대체 이름 사용
        auto_trading = os.path.join(home, 'auto_trading')
        os.makedirs(auto_trading, exist_ok=True)
        print(f"[OK] auto_trading folder created: {auto_trading}")
    
    return kis_config, auto_trading

if __name__ == '__main__':
    setup_folders()
