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
  pageTitle?: string;
  headline?: string;
  subtitle: string;
  description: string;
  heroQuote: string;
  quote?: string;
  sectionTitle?: string;
  manifestoTitle: string;
  manifestoText: string;
  bodyText?: string;
  body?: string;
  pillarsTitle: string;
  pillars: AboutPillar[];
  seo: AboutSeo;
  updatedAt?: string;
}
