export interface ApplyHeader {
  title: string;
  subtitle: string;
  description: string;
}

export interface ApplyFaq {
  id: string;
  question: string;
  answer: string;
  displayOrder: number;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ApplyFaqCreateUpdate {
  question: string;
  answer: string;
  displayOrder?: number;
  isActive?: boolean;
}

export interface ApplyFaqReorder {
  items: { id: string; displayOrder: number }[];
}
