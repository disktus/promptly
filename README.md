Markdown

### 백엔드 환경 설정 및 실행 (로컬 에뮬레이터 가이드)

**필수 준비 사항:** Node.js (v22 이상 권장) 및 Java (JDK 11 이상)가 시스템 PATH에 등록되어 있어야함.

만약 PATH 경로를 못잡을 시
-> 시스템 환경 변수 편집 > 환경 변수 > 시스템 변수에서 PATH 찾아 클릭 > 새로 만들기 > jdk-25.0.1.8(버전은 다를 수 있음, jdk-버전)/bin 경로 복사 후 추가

#### 1.루트 디렉토리의 local.properties에 api키 추가

GEMINI_API_KEY=AIzaSyDMGN_dYpaq-AZsyrBVYJEqDw9O5Egj2G4

#### 2. Functions 모듈 설치 및 빌드

프로젝트 루트 디렉토리에서 아래 명령어를 순서대로 실행해 서버 코드를 준비

```bash
# 1. Functions 디렉토리로 이동하여 Node 모듈 설치
cd functions
npm install

# 2. TypeScript 코드를 JavaScript로 빌드 (Functions 실행 파일 준비)
npm run build

# 3. 프로젝트 루트로 복귀
cd ..
2. 에뮬레이터 구동 및 데이터 주입
Functions와 Firestore 에뮬레이터가 정상적으로 로드될 수 있도록 firebase.json 파일을 확인한 후, 아래 명령어를 실행.

에뮬레이터 실행:

Firestore와 Functions가 모두 실행됩니다. 이 터미널 창은 절대로 닫지 마십시오.

Bash

firebase emulators:start
문제 데이터 주입 (Seed):

에뮬레이터가 켜진 상태에서 새로운 터미널 창을 열고 실행하여 DB에 초기 문제를 추가.

Bash

node scripts/seed.js
3. Android App 실행
백엔드 환경(Functions on 5001, Firestore on 8080)이 모두 구동 중인 상태에서 Android Studio를 통해 앱을 실행하면 로컬 서버와 연동. (이때 서버가 켜져있는 bash창은 꼭 켜두어야함.)
