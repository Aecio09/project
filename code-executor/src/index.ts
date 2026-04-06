import express, { Request, Response } from 'express';

const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

app.get('/health', (req: Request, res: Response) => {
  res.json({ status: 'ok', service: 'code-executor' });
});

app.listen(PORT, () => {
  console.log(`Code executor service running on port ${PORT}`);
});
