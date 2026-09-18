import type { Metadata } from 'next';
import './globals.css';
import { Header } from '@/components/layout/Header';
import { Footer } from '@/components/layout/Footer';

export const metadata: Metadata = {
  title: 'WB Scouting | Agência de Modelos & Casting B2B',
  description: 'Plataforma oficial de casting e agenciamento WB Scouting. Estética editorial minimalista, talentos Stars e funil de scouting inteligente.',
  keywords: ['WB Scouting', 'Modelos', 'Casting', 'São Paulo', 'Agência de Modelos', 'Fashion', 'Stars'],
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="pt-BR">
      <body className="flex min-h-screen flex-col bg-white text-black">
        <Header />
        <main className="flex-grow">{children}</main>
        <Footer />
      </body>
    </html>
  );
}
