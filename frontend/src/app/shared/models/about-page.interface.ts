export interface AboutPillar {
  order: number;
  titulo: string;
  descricao: string;
}

export interface AboutSeo {
  metaTitle: string;
  metaDescription: string;
}

export interface AboutPage {
  title: string;
  subtitle: string;
  description: string;
  heroQuote: string;
  manifestoTitle: string;
  manifestoText: string;
  pillarsTitle: string;
  pillars: AboutPillar[];
  seo: AboutSeo;
  updatedAt?: string;
}
