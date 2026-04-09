import express, { Request, Response } from 'express';

const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

interface AnswerVerificationRequestDto {
  answerId: number;
  questionId: number;
  answerBody: string;
  questionBody: string;
  questionType: string;
  difficulty: string;
  requiredUsage: string | null;
  topic: string;
}

interface VerificationResult {
  valid: boolean;
  message: string;
  requiredUsage: string | null;
  found: boolean;
}

app.get('/health', (req: Request, res: Response) => {
  res.json({ status: 'ok', service: 'code-executor' });
});

app.post('/verify-answer', (req: Request, res: Response) => {
  try {
    const dto: AnswerVerificationRequestDto = req.body;

    if (!dto.answerBody) {
      return res.status(400).json({
        valid: false,
        message: 'Answer body is required',
        requiredUsage: dto.requiredUsage,
        found: false
      });
    }

    if (!dto.requiredUsage) {
      return res.json({
        valid: true,
        message: 'No required usage to verify',
        requiredUsage: null,
        found: true
      });
    }

    const result = verifyRequiredUsage(dto.answerBody, dto.requiredUsage);
    res.json(result);
  } catch (error) {
    console.error('Error verifying answer:', error);
    res.status(500).json({
      valid: false,
      message: 'Internal server error',
      requiredUsage: null,
      found: false
    });
  }
});

function verifyRequiredUsage(answerBody: string, requiredUsage: string): VerificationResult {
  const normalizedAnswer = answerBody.toLowerCase();
  const normalizedUsage = requiredUsage.toUpperCase();

  let found = false;
  let message = '';

  switch (normalizedUsage) {
    case 'WHILE':
      found = /\bwhile\s*\(/.test(normalizedAnswer);
      message = found 
        ? 'Answer correctly uses WHILE loop'
        : 'Answer does not contain required WHILE loop';
      break;

    case 'FOR':
      found = /\bfor\s*\(/.test(normalizedAnswer);
      message = found
        ? 'Answer correctly uses FOR loop'
        : 'Answer does not contain required FOR loop';
      break;

    case 'IF':
      found = /\bif\s*\(/.test(normalizedAnswer);
      message = found
        ? 'Answer correctly uses IF statement'
        : 'Answer does not contain required IF statement';
      break;

    default:
      return {
        valid: false,
        message: `Unknown required usage: ${requiredUsage}`,
        requiredUsage: requiredUsage,
        found: false
      };
  }

  return {
    valid: found,
    message: message,
    requiredUsage: requiredUsage,
    found: found
  };
}

app.listen(PORT, () => {
  console.log(`Code executor service running on port ${PORT}`);
});
