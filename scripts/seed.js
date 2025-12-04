// scripts/seed.js
// Firestore 에뮬레이터에 questions 3개 시드 삽입
// 1) 에뮬레이터 주소 지정 (기본 8080)
process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8080";

const admin = require("firebase-admin");

const PROJECT_ID = process.env.FB_PROJECT_ID || "promptly-trycatch"; // 필요시 환경변수로 바꿔도 됨

admin.initializeApp({ projectId: PROJECT_ID });
const db = admin.firestore();

async function main() {
  const now = admin.firestore.FieldValue.serverTimestamp();
  // 문서 ID 고정(q1~q3) — 여러 번 돌려도 덮어쓰기만 일어나게
  const batch = db.batch();

  const q1 = db.collection("questions").doc("q1");
  batch.set(q1, {
    situation: "학교 과제",
    job: "대학생",
    work: "보고서",
    style: "객관적/논리적",
    type: "text",
    createdAt: now,
  }, { merge: true });

  const q2 = db.collection("questions").doc("q2");
  batch.set(q2, {
    situation: "숲 속",
    job: "",
    work: "",
    style: "평화로움",
    type: "image",
    createdAt: now,
  }, { merge: true });

  const q3 = db.collection("questions").doc("q3");
  batch.set(q3, {
    situation: "주말 요리",
    job: "자취생생",
    work: "레시피",
    style: "간단하게",
    type: "text",
    createdAt: now,
  }, { merge: true });

  await batch.commit();

  console.log("✅ Seed completed: questions q1, q2, q3 inserted/updated.");
  process.exit(0);
}

main().catch((e) => {
  console.error("❌ Seed failed:", e);
  process.exit(1);
});
