// functions/src/ai/openaiScore.ts
import OpenAI from "openai";

export type ScoreJSON = {
  clarity: number; specificity: number; format: number; role: number; context: number;
  feedback: string;
};

// 키는 모듈 최상단에서 읽지 말고, "호출 시점"에 인자로 받는다.
export async function scoreWithOpenAI(
  apiKey: string,
  userAnswer: string,
  question: any
): Promise<ScoreJSON> {
  if (!apiKey) throw new Error("OPENAI_API_KEY_MISSING");

  const client = new OpenAI({ apiKey });

  const prompt = `
너는 프롬프트 품질 평가관이다.
평가기준: 
clarity(명확성), specificity(구체성), format(형식), role(역할), context(맥락)
각 항목은 0~100점으로 채점하여 5가지 항목의 평균점수를 내고, 마지막에 한국어 짧은 총평을 feedback에 작성해라.

1️. 명확성 (Clarity)
-의도가 분명하고, 문장 구조가 이해하기 쉬운가

평가 포인트:
요청 목적이 한눈에 드러나는가
문장 구조가 논리적으로 연결되는가
애매하거나 중의적인 표현이 없는가
가점 예시 문구: “~을(를) 설명해줘”, “목표는 ~이다”, “결과물은 ~해야 한다”
감점 예시: “대충”, “적당히”, “알아서”, “좋게 해줘”

채점 가이드
0–40: 문장 구조가 불분명하거나 요구사항이 모호함
41–70: 대략적 방향은 있으나 일부 모호함 존재
71–90: 의도가 명확하고 흐름이 일관됨
91–100: 누가 봐도 오해 없이 단일한 목적이 드러남

2️. 구체성 (Specificity)
-요구사항이 세부적이며 실행 가능하게 명시되어 있는가

평가 포인트:
필요한 세부 조건(예: 길이, 내용 범위, 단계 수)이 제시됨
불필요하게 추상적인 단어 대신 구체적 기준이 있음
답변자가 “무엇을 얼마나 해야 하는지” 바로 파악 가능함
가점 예시 문구: “3단계로 설명”, “예시는 2개 이상”, “300자 이내”
감점 예시: “자세히 써줘” (세부 기준 없음)

채점 가이드
0–40: 세부 기준 전무, 추상적 지시
41–70: 일부 구체화되었으나 불완전
71–90: 명확한 수치·조건 포함
91–100: 실행 가능한 단계나 결과물이 완전하게 정의됨

3️.형식 (Format)
출력 구조나 표현 방식이 명시되어 있는가

평가 포인트:
답변이 어떤 형태(문단, 목록, 표 등)로 나와야 하는지 제시됨
필요시 출력 구조(예: JSON, 마크다운, 제목/본문 구분)가 일관적임
불필요한 형식 혼란이 없음
가점 예시 문구: “표로 정리”, “리스트로 작성”, “답안 구조는 다음과 같다”
감점 예시: 아무 형식 언급 없이 문장 나열

채점 가이드
0–40: 형식 지침 전혀 없음
41–70: 대략적 형식만 암시됨
71–90: 구조가 명확히 정의됨
91–100: 일관된 형식·레이아웃까지 구체적으로 제시됨

4️.역할 (Role)
AI 또는 답변자의 역할과 태도가 분명히 제시되어 있는가

평가 포인트:
답변자가 어떤 입장에서 말해야 하는지 명시됨
청중 수준에 맞는 말투·어휘 선택이 가능하도록 지시됨
문체나 태도(공식적/친근함/객관적 등)가 일관됨
가점 예시 문구: “너는 ~전문가야”, “학생에게 설명하듯”, “강의하듯 정중히”
감점 예시: 역할·청중 언급 없이 일반적 지시만 존재

채점 가이드
0–40: 역할·톤·대상 전혀 없음
41–70: 직함 또는 청중 중 하나만 있음
71–90: 역할·톤·대상 모두 언급
91–100: 일관된 역할·태도 유지 + 청중 수준에 적합

5️. 맥락 (Context)
요청 배경, 상황, 목적이 자연스럽게 포함되어 있는가

평가 포인트:
작업 목적이나 사용 배경이 드러남
문제나 주제의 전후 관계가 제시됨
프롬프트가 고립된 문장이 아니라 전체 흐름 속에서 작동함
가점 예시 문구: “이전 질문의 연속으로”, “수업 발표 준비용”, “시험험 대비”
감점 예시: 아무 배경 없이 명령만 존재

 채점 가이드
0–40: 배경 전혀 없음
41–70: 일부 맥락은 있으나 불충분
71–90: 목적·상황이 자연스럽게 연결
91–100: 배경·목적·전제조건이 모두 명확히 제시됨

Feedback (총평)
한 줄 또는 두 줄의 간단한 한국어 피드백으로 종합 판단


[문항]
상황: ${question.situation}
직업: ${question.job}
작성물: ${question.work}
스타일: ${question.style}

[사용자 답안]
${userAnswer}

반드시 아래 JSON만 출력:
{"clarity":0,"specificity":0,"format":0,"role":0,"context":0,"feedback":"..."}
`;

  const completion = await client.chat.completions.create({
    model: "gpt-4o-mini",
    messages: [{ role: "user", content: prompt }],
    temperature: 0.2,
  });

  const text = completion.choices[0]?.message?.content ?? "{}";
  const parsed = JSON.parse(text);
  const clamp = (x:number)=>Math.max(0, Math.min(5, Number(x)||0));

  return {
    clarity: clamp(parsed.clarity),
    specificity: clamp(parsed.specificity),
    format: clamp(parsed.format),
    role: clamp(parsed.role),
    context: clamp(parsed.context),
    feedback: String(parsed.feedback ?? ""),
  };
}
