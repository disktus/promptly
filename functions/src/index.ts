// functions/src/index.ts
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { z } from "zod";
import { normalizeNickname } from "./utils/normalizeNickname";
import { scoreWithOpenAI } from "./ai/openaiScore";

admin.initializeApp();
const db = admin.firestore();

// 요청 시점에 OpenAI 키 읽기 (config 우선, 없으면 .env)
function getOpenAIKey(): string {
  const fromConfig =
    (functions.config()?.openai && functions.config().openai.key) || "";
  const fromEnv = process.env.OPENAI_API_KEY || "";
  return fromConfig || fromEnv;
}

/** 헬스체크 */
export const ping = functions.https.onRequest(async (_req, res) => {
  res.json({ ok: true, now: Date.now() });
  return;
});

/** 1) 닉네임 중복 체크 (GET) */
export const checkNickname = functions.https.onRequest(async (req, res) => {
  try {
    const name = String(req.query.name ?? "");
    const norm = normalizeNickname(name);
    if (!norm.ok) {
      res.status(400).json({ available: false, reason: norm.reason });
      return;
    }
    const doc = await db.doc(`nicknames/${norm.key}`).get();
    res.json({ available: !doc.exists, normalized: norm.key });
    return;
  } catch (e) {
    console.error("checkNickname error:", e);
    res.status(500).json({ available: false, reason: "SERVER_ERROR" });
    return;
  }
});

/** 2) 닉네임 확정 (POST) */
export const claimNickname = functions.https.onRequest(async (req, res) => {
  try {
    const { name } = req.body ?? {};
    const norm = normalizeNickname(String(name ?? ""));
    if (!norm.ok) {
      res.status(400).json({ ok: false, reason: norm.reason });
      return;
    }

    const nickRef = db.doc(`nicknames/${norm.key}`);
    const userRef = db.collection("users").doc();

    try {
      await db.runTransaction(async (tx) => {
        const snap = await tx.get(nickRef);
        if (snap.exists)
          throw new functions.https.HttpsError("already-exists", "DUPLICATE");
        tx.create(nickRef, { userId: userRef.id });
        tx.create(userRef, {
          nickname: norm.pretty,
          nicknameKey: norm.key,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      });

      res.json({ ok: true, userId: userRef.id });
      return;
    } catch (e: any) {
      if (e?.code === "already-exists") {
        res.status(409).json({ ok: false, reason: "DUPLICATE" });
        return;
      }
      console.error("claimNickname txn error:", e);
      res.status(500).json({ ok: false, reason: "SERVER_ERROR" });
      return;
    }
  } catch (e) {
    console.error("claimNickname error:", e);
    res.status(500).json({ ok: false, reason: "SERVER_ERROR" });
    return;
  }
});

/** 3) 레벨테스트 3문항 조회 (GET) */
export const levelQuestions = functions.https.onRequest(async (_req, res) => {
  try {
    const qs = await db
      .collection("questions")
      .orderBy("createdAt", "asc")
      .limit(3)
      .get();
    const list = qs.docs.map((d) => ({ id: d.id, ...d.data() }));
    res.json(list);
    return;
  } catch (e) {
    console.error("levelQuestions error:", e);
    res.status(500).json({ ok: false, reason: "SERVER_ERROR" });
    return;
  }
});

/** 4) 답안 채점 + 저장 (POST) */
export const scoreAnswer = functions.https.onRequest(async (req, res) => {
  try {
    const body = z
      .object({
        userId: z.string().min(1),
        questionId: z.string().min(1),
        content: z.string().min(1),
      })
      .parse(req.body ?? {});

    const qSnap = await db.doc(`questions/${body.questionId}`).get();
    if (!qSnap.exists) {
      res.status(404).json({ ok: false, reason: "QUESTION_NOT_FOUND" });
      return;
    }

    const apiKey = getOpenAIKey();
    if (!apiKey) {
      res.status(500).json({ ok: false, reason: "OPENAI_KEY_MISSING" });
      return;
    }

    const scores = await scoreWithOpenAI(apiKey, body.content, qSnap.data());
    const avg = Number(
      (
        (scores.clarity +
          scores.specificity +
          scores.format +
          scores.role +
          scores.context) /
        5
      ).toFixed(2)
    );

    const ansRef = db.collection("answers").doc();
    await ansRef.set({
      userId: body.userId,
      questionId: body.questionId,
      content: body.content,
      scores,
      average: avg,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    res.json({ ok: true, average: avg, scores, answerId: ansRef.id });
    return;
  } catch (e) {
    console.error("scoreAnswer error:", e);
    res.status(502).json({ ok: false, reason: "AI_OR_SERVER_ERROR" });
    return;
  }
});
