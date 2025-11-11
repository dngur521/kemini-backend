-- 애플리케이션 실행 시 security_questions 테이블에 기본 질문 데이터를 삽입합니다.

-- (테이블이 비어있을 때만 실행되도록)
INSERT INTO security_questions (id, question_text)
SELECT 1, '졸업한 초등학교 이름은?'
WHERE NOT EXISTS (SELECT 1 FROM security_questions WHERE id = 1);

INSERT INTO security_questions (id, question_text)
SELECT 2, '어머니의 성함은?'
WHERE NOT EXISTS (SELECT 1 FROM security_questions WHERE id = 2);

INSERT INTO security_questions (id, question_text)
SELECT 3, '가장 친한 친구의 이름은?'
WHERE NOT EXISTS (SELECT 1 FROM security_questions WHERE id = 3);

INSERT INTO security_questions (id, question_text)
SELECT 4, '가장 기억에 남는 여행지는?'
WHERE NOT EXISTS (SELECT 1 FROM security_questions WHERE id = 4);

INSERT INTO security_questions (id, question_text)
SELECT 5, '처음으로 키운 반려동물의 이름은?'
WHERE NOT EXISTS (SELECT 1 FROM security_questions WHERE id = 5);

INSERT INTO security_questions (id, question_text)
SELECT 6, '가장 존경하는 인물의 이름은?'
WHERE NOT EXISTS (SELECT 1 FROM security_questions WHERE id = 6);

INSERT INTO security_questions (id, question_text)
SELECT 7, '어릴 적 살던 동네(거리) 이름은?'
WHERE NOT EXISTS (SELECT 1 FROM security_questions WHERE id = 7);

INSERT INTO security_questions (id, question_text)
SELECT 8, '가장 좋아하는 책/영화의 제목은?'
WHERE NOT EXISTS (SELECT 1 FROM security_questions WHERE id = 8);