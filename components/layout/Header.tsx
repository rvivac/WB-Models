'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

export const Header: React.FC = () => {
  const pathname = usePathname();
  const [lang, setLang] = useState<'PT' | 'EN'>('PT');
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const navLinks = [
    { href: '/casting', label: lang === 'PT' ? 'Casting' : 'Casting' },
    { href: '/stars', label: lang === 'PT' ? 'Stars' : 'Stars' },
    { href: '/scouting', label: lang === 'PT' ? 'Quero ser Modelo' : 'Be a Model' },
    { href: '/agency', label: lang === 'PT' ? 'A Agência' : 'About Us' },
    { href: '/contact', label: lang === 'PT' ? 'Contato' : 'Contact' },
  ];

  return (
    <header className="sticky top-0 z-50 w-full bg-white/95 backdrop-blur-sm border-b border-neutral-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-20 flex items-center justify-between">
        {/* Marca Editorial Minimalista */}
        <Link href="/" className="flex flex-col">
          <span className="text-xl sm:text-2xl font-black uppercase tracking-tighter text-black font-sans">
            WB Scouting
          </span>
          <span className="text-[9px] font-mono tracking-widest text-neutral-500 uppercase -mt-1">
            Casting & Management
          </span>
        </Link>

        {/* Navegação Desktop */}
        <nav className="hidden md:flex items-center gap-8 text-xs font-mono uppercase tracking-widest text-neutral-800">
          {navLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={`transition-colors duration-200 hover:text-black ${
                pathname === link.href ? 'text-black font-bold border-b border-black pb-0.5' : 'text-neutral-600'
              }`}
            >
              {link.label}
            </Link>
          ))}
        </nav>

        {/* Utilitários: Seletor de Idioma & Backoffice */}
        <div className="hidden md:flex items-center gap-4 text-xs font-mono">
          <div className="flex border border-neutral-300">
            <button
              type="button"
              onClick={() => setLang('PT')}
              className={`px-2 py-1 transition-colors ${
                lang === 'PT' ? 'bg-black text-white' : 'text-neutral-600 hover:text-black'
              }`}
            >
              PT
            </button>
            <button
              type="button"
              onClick={() => setLang('EN')}
              className={`px-2 py-1 transition-colors ${
                lang === 'EN' ? 'bg-black text-white' : 'text-neutral-600 hover:text-black'
              }`}
            >
              EN
            </button>
          </div>

          <Link
            href="/admin"
            className="border border-neutral-900 px-3 py-1 text-[11px] uppercase tracking-wider text-black hover:bg-black hover:text-white transition-colors"
          >
            Acesso Booker
          </Link>
        </div>

        {/* Botão Mobile */}
        <div className="md:hidden flex items-center gap-3">
          <button
            type="button"
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            className="p-2 text-black border border-neutral-300 text-xs font-mono uppercase"
          >
            {isMobileMenuOpen ? 'FECHAR' : 'MENU'}
          </button>
        </div>
      </div>

      {/* Menu Mobile */}
      {isMobileMenuOpen && (
        <div className="md:hidden border-t border-neutral-200 bg-white px-4 pt-4 pb-6 space-y-3">
          {navLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              onClick={() => setIsMobileMenuOpen(false)}
              className="block text-sm font-mono uppercase tracking-wider text-black py-2 border-b border-neutral-100"
            >
              {link.label}
            </Link>
          ))}
          <div className="pt-4 flex justify-between items-center">
            <div className="flex border border-neutral-300">
              <button
                type="button"
                onClick={() => setLang('PT')}
                className={`px-3 py-1 text-xs font-mono ${lang === 'PT' ? 'bg-black text-white' : 'text-neutral-600'}`}
              >
                PT
              </button>
              <button
                type="button"
                onClick={() => setLang('EN')}
                className={`px-3 py-1 text-xs font-mono ${lang === 'EN' ? 'bg-black text-white' : 'text-neutral-600'}`}
              >
                EN
              </button>
            </div>
            <Link
              href="/admin"
              onClick={() => setIsMobileMenuOpen(false)}
              className="text-xs font-mono uppercase tracking-wider underline"
            >
              Acesso Booker
            </Link>
          </div>
        </div>
      )}
    </header>
  );
};
