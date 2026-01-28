# -*- coding: utf-8 -*-
"""가상환경 설정 스크립트"""
import os
import sys
import subprocess

# Windows 콘솔 인코딩 설정
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

def setup_venv():
    """가상환경 설정"""
    home = os.path.expanduser('~')
    auto_trading = os.path.join(home, '자동매매')
    
    if not os.path.exists(auto_trading):
        print(f"[ERROR] 자동매매 folder not found: {auto_trading}")
        return False
    
    # 디렉토리 변경
    os.chdir(auto_trading)
    print(f"[OK] Changed directory to: {auto_trading}")
    
    # pyproject.toml 확인
    if not os.path.exists('pyproject.toml'):
        print("[ERROR] pyproject.toml not found")
        return False
    
    print("[OK] pyproject.toml found")
    
    # uv sync 실행
    print("[INFO] Running uv sync...")
    try:
        result = subprocess.run(['uv', 'sync'], check=True, capture_output=True, text=True, encoding='utf-8', errors='replace')
        print(result.stdout)
        print("[OK] Virtual environment setup completed")
        return True
    except subprocess.CalledProcessError as e:
        print(f"[ERROR] uv sync failed: {e}")
        print(e.stderr)
        return False
    except FileNotFoundError:
        print("[ERROR] uv command not found. Please install uv first.")
        return False

if __name__ == '__main__':
    setup_venv()
