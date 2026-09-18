'use client';

import React, { useState } from 'react';
import { ModelGender, EyeColor, HairColor } from '@/types/database.types';
import { compressImageClientSide } from '@/lib/utils/image-compression';
import { supabase } from '@/lib/supabase/client';
import { EYE_COLOR_LABELS, HAIR_COLOR_LABELS } from '@/lib/utils/formatters';

interface UploadedPhotoPreview {
  file: File;
  previewUrl: string;
  originalSizeKb: number;
  compressedSizeKb: number;
  label: string;
}

export const ScoutingFunnel: React.FC = () => {
  const [step, setStep] = useState<1 | 2 | 3 | 4>(1);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCompressing, setIsCompressing] = useState(false);
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Etapa 1: Identificação
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phoneWhatsapp, setPhoneWhatsapp] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');

  // Etapa 2: Medidas Corporais
  const [gender, setGender] = useState<ModelGender>('female');
  const [heightCm, setHeightCm] = useState<number>(175);
  const [bustChestCm, setBustChestCm] = useState<number>(85);
  const [waistCm, setWaistCm] = useState<number>(62);
  const [hipsCm, setHipsCm] = useState<number>(90);
  const [shoeSize, setShoeSize] = useState<number>(38);
  const [dressSize, setDressSize] = useState<string>('36');
  const [eyeColor, setEyeColor] = useState<EyeColor>('castanho_escuro');
  const [hairColor, setHairColor] = useState<HairColor>('castanho_escuro');

  // Etapa 3: Redes Sociais & Contato Adicional
  const [instagram, setInstagram] = useState('');

  // Etapa 4: Fotos & LGPD
  const [photos, setPhotos] = useState<UploadedPhotoPreview[]>([]);
  const [lgpdConsent, setLgpdConsent] = useState(false);

  // Manipulação e Compressão no Browser
  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    if (photos.length + files.length > 8) {
      alert('Limite máximo de 8 fotos por candidatura.');
      return;
    }

    setIsCompressing(true);
    setErrorMessage(null);

    try {
      const newPhotos: UploadedPhotoPreview[] = [];

      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const originalSizeKb = Math.round(file.size / 1024);

        // Compressão client-side (Max 1920px, WebP, 0.82 quality)
        const compressedFile = await compressImageClientSide(file, {
          maxDimension: 1920,
          quality: 0.82,
          outputFormat: 'image/webp',
        });

        const compressedSizeKb = Math.round(compressedFile.size / 1024);
        const previewUrl = URL.createObjectURL(compressedFile);

        newPhotos.push({
          file: compressedFile,
          previewUrl,
          originalSizeKb,
          compressedSizeKb,
          label: `Foto ${photos.length + i + 1}`,
        });
      }

      setPhotos((prev) => [...prev, ...newPhotos]);
    } catch (err: any) {
      setErrorMessage(err.message || 'Erro ao processar as fotos.');
    } finally {
      setIsCompressing(false);
    }
  };

  const removePhoto = (index: number) => {
    setPhotos((prev) => prev.filter((_, idx) => idx !== index));
  };

  // Submissão Segura ao Supabase
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!lgpdConsent) {
      setErrorMessage('Você deve aceitar os termos de consentimento e LGPD para prosseguir.');
      return;
    }
    if (photos.length === 0) {
      setErrorMessage('Por favor, adicione pelo menos uma foto de rosto e uma de corpo inteiro.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const uploadedUrls: { url: string; label: string; name: string }[] = [];

      // 1. Upload das fotos comprimidas para o bucket 'scouting-quarantine'
      for (const item of photos) {
        const fileExt = 'webp';
        const fileName = `${Date.now()}-${Math.random().toString(36).substring(2, 8)}.${fileExt}`;
        const filePath = `candidatures/${fileName}`;

        const { data: uploadData, error: uploadError } = await supabase.storage
          .from('scouting-quarantine')
          .upload(filePath, item.file, {
            cacheControl: '3600',
            upsert: false,
          });

        if (uploadError) throw uploadError;

        uploadedUrls.push({
          url: filePath,
          label: item.label,
          name: fileName,
        });
      }

      // 2. Gravação do Registro com Consentimento LGPD
      const { error: insertError } = await supabase.from('candidatures').insert({
        full_name: fullName,
        email,
        phone_whatsapp: phoneWhatsapp,
        birth_date: birthDate,
        instagram: instagram || null,
        city,
        state,
        gender,
        height_cm: heightCm,
        bust_chest_cm: bustChestCm,
        waist_cm: waistCm,
        hips_cm: hipsCm,
        shoe_size: shoeSize,
        dress_size: dressSize,
        eye_color: eyeColor,
        hair_color: hairColor,
        uploaded_photos: uploadedUrls,
        lgpd_consent_given: true,
        lgpd_consent_timestamp: new Date().toISOString(),
        lgpd_consent_user_agent: typeof window !== 'undefined' ? window.navigator.userAgent : null,
      });

      if (insertError) throw insertError;

      setSubmitSuccess(true);
    } catch (err: any) {
      setErrorMessage(err.message || 'Ocorreu um erro ao enviar sua ficha. Tente novamente.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (submitSuccess) {
    return (
      <div className="max-w-2xl mx-auto p-12 text-center border border-black bg-white my-12">
        <div className="text-xs font-mono tracking-widest text-neutral-500 uppercase mb-2">
          WB SCOUTING | REGISTRO CONFIRMADO
        </div>
        <h2 className="text-3xl font-black uppercase tracking-tight text-black mb-4">
          Ficha Recebida com Sucesso
        </h2>
        <p className="text-sm font-sans text-neutral-600 mb-6 leading-relaxed">
          Sua candidatura foi registrada de forma segura em nosso sistema de Scouting. Nossa equipe
          de bookers avaliará seu perfil e medidas. Caso seu biótipo atenda às demandas de nossos
          clientes e campanhas atuais, entraremos em contato diretamente pelo WhatsApp informado.
        </p>
        <div className="p-4 bg-neutral-50 border border-neutral-200 text-xs font-mono text-neutral-500 text-left">
          <strong>Protocolo LGPD:</strong> Seus dados biométricos e fotos estão armazenados em
          quarentena criptografada e serão automaticamente descartados em 90 dias caso não haja
          aproveitamento no casting.
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto p-6 sm:p-10 bg-white border border-neutral-300 my-8">
      {/* Indicador de Etapas Editorial */}
      <div className="border-b border-black pb-4 mb-8">
        <div className="flex justify-between items-center mb-2">
          <span className="text-[10px] font-mono tracking-widest text-neutral-500 uppercase">
            WB TALENT ACQUISITION
          </span>
          <span className="text-xs font-mono font-bold">ETAPA 0{step} / 04</span>
        </div>
        <div className="flex gap-1 h-1 bg-neutral-100">
          <div className={`h-full transition-all duration-300 ${step >= 1 ? 'bg-black flex-1' : 'flex-1'}`} />
          <div className={`h-full transition-all duration-300 ${step >= 2 ? 'bg-black flex-1' : 'flex-1'}`} />
          <div className={`h-full transition-all duration-300 ${step >= 3 ? 'bg-black flex-1' : 'flex-1'}`} />
          <div className={`h-full transition-all duration-300 ${step >= 4 ? 'bg-black flex-1' : 'flex-1'}`} />
        </div>
      </div>

      {errorMessage && (
        <div className="mb-6 p-4 bg-red-50 border-l-2 border-red-600 text-xs font-mono text-red-700">
          {errorMessage}
        </div>
      )}

      {/* FORMULÁRIO MULTI-ETAPAS */}
      <form onSubmit={handleSubmit}>
        {/* ETAPA 1: Identificação */}
        {step === 1 && (
          <div className="space-y-4">
            <h2 className="text-xl font-bold uppercase tracking-tight text-black mb-6">
              01. Identificação Básica
            </h2>
            <div>
              <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                Nome Completo *
              </label>
              <input
                type="text"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
              />
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                  E-mail *
                </label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                  WhatsApp com DDD *
                </label>
                <input
                  type="tel"
                  required
                  placeholder="(11) 99999-9999"
                  value={phoneWhatsapp}
                  onChange={(e) => setPhoneWhatsapp(e.target.value)}
                  className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
                />
              </div>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                  Data de Nascimento *
                </label>
                <input
                  type="date"
                  required
                  value={birthDate}
                  onChange={(e) => setBirthDate(e.target.value)}
                  className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                  Cidade *
                </label>
                <input
                  type="text"
                  required
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                  Estado (UF) *
                </label>
                <input
                  type="text"
                  required
                  maxLength={2}
                  placeholder="SP"
                  value={state}
                  onChange={(e) => setState(e.target.value.toUpperCase())}
                  className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
                />
              </div>
            </div>
            <div className="pt-6 flex justify-end">
              <button
                type="button"
                onClick={() => {
                  if (!fullName || !email || !phoneWhatsapp || !birthDate || !city || !state) {
                    setErrorMessage('Por favor, preencha todos os campos obrigatórios.');
                    return;
                  }
                  setErrorMessage(null);
                  setStep(2);
                }}
                className="bg-black text-white px-6 py-2.5 text-xs font-mono uppercase tracking-widest hover:bg-neutral-800"
              >
                Avançar: Medidas &rarr;
              </button>
            </div>
          </div>
        )}

        {/* ETAPA 2: Medidas Corporais */}
        {step === 2 && (
          <div className="space-y-4">
            <h2 className="text-xl font-bold uppercase tracking-tight text-black mb-6">
              02. Biometria & Medidas
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                  Gênero *
                </label>
                <select
                  value={gender}
                  onChange={(e) => setGender(e.target.value as ModelGender)}
                  className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
                >
                  <option value="female">Feminino</option>
                  <option value="male">Masculino</option>
                  <option value="non_binary">Não-binário</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                  Altura (cm) *
                </label>
                <input
                  type="number"
                  required
                  min={140}
                  max={220}
                  value={heightCm}
                  onChange={(e) => setHeightCm(Number(e.target.value))}
                  className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
                />
              </div>
            </div>
            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                  Busto/Tórax (cm)
                </label>
                <input
                  type="number"
                  value={bustChestCm}
                  onChange={(e) => setBustChestCm(Number(e.target.value))}
                  className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                  Cintura (cm)
                </label>
                <input
                  type="number"
                  value={waistCm}
                  onChange={(e) => setWaistCm(Number(e.target.value))}
                  className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                  Quadril (cm)
                </label>
                <input
                  type="number"
                  value={hipsCm}
                  onChange={(e) => setHipsCm(Number(e.target.value))}
                  className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
                />
              </div>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                  Calçado
                </label>
                <input
                  type="number"
                  value={shoeSize}
                  onChange={(e) => setShoeSize(Number(e.target.value))}
                  className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                  Manequim
                </label>
                <input
                  type="text"
                  value={dressSize}
                  onChange={(e) => setDressSize(e.target.value)}
                  className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                  Cor dos Olhos
                </label>
                <select
                  value={eyeColor}
                  onChange={(e) => setEyeColor(e.target.value as EyeColor)}
                  className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
                >
                  {Object.entries(EYE_COLOR_LABELS).map(([k, v]) => (
                    <option key={k} value={k}>{v}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                  Cor do Cabelo
                </label>
                <select
                  value={hairColor}
                  onChange={(e) => setHairColor(e.target.value as HairColor)}
                  className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
                >
                  {Object.entries(HAIR_COLOR_LABELS).map(([k, v]) => (
                    <option key={k} value={k}>{v}</option>
                  ))}
                </select>
              </div>
            </div>
            <div className="pt-6 flex justify-between">
              <button
                type="button"
                onClick={() => setStep(1)}
                className="border border-neutral-300 px-6 py-2.5 text-xs font-mono uppercase tracking-widest text-neutral-700 hover:border-black"
              >
                &larr; Voltar
              </button>
              <button
                type="button"
                onClick={() => setStep(3)}
                className="bg-black text-white px-6 py-2.5 text-xs font-mono uppercase tracking-widest hover:bg-neutral-800"
              >
                Avançar: Redes &rarr;
              </button>
            </div>
          </div>
        )}

        {/* ETAPA 3: Redes Sociais */}
        {step === 3 && (
          <div className="space-y-4">
            <h2 className="text-xl font-bold uppercase tracking-tight text-black mb-6">
              03. Presença Digital
            </h2>
            <div>
              <label className="block text-xs font-mono uppercase tracking-wider text-neutral-600 mb-1">
                Instagram Oficial (@usuario)
              </label>
              <input
                type="text"
                placeholder="@seu.perfil"
                value={instagram}
                onChange={(e) => setInstagram(e.target.value)}
                className="w-full border border-neutral-300 p-2 text-sm font-sans focus:border-black focus:outline-none"
              />
              <span className="text-[10px] font-mono text-neutral-400 mt-1 block">
                * Mantenha seu perfil aberto durante o período de avaliação do scouting.
              </span>
            </div>
            <div className="pt-6 flex justify-between">
              <button
                type="button"
                onClick={() => setStep(2)}
                className="border border-neutral-300 px-6 py-2.5 text-xs font-mono uppercase tracking-widest text-neutral-700 hover:border-black"
              >
                &larr; Voltar
              </button>
              <button
                type="button"
                onClick={() => setStep(4)}
                className="bg-black text-white px-6 py-2.5 text-xs font-mono uppercase tracking-widest hover:bg-neutral-800"
              >
                Avançar: Polaroids & Fotos &rarr;
              </button>
            </div>
          </div>
        )}

        {/* ETAPA 4: Upload de Fotos & LGPD */}
        {step === 4 && (
          <div className="space-y-6">
            <div>
              <h2 className="text-xl font-bold uppercase tracking-tight text-black mb-2">
                04. Fotos Naturais & Polaroids
              </h2>
              <p className="text-xs font-sans text-neutral-600">
                Envie fotos naturais, sem maquiagem pesada ou filtros de aplicativo. Recomendamos:
                1 rosto de frente, 1 perfil, 1 corpo inteiro e fotos livres (máximo 8 fotos).
              </p>
            </div>

            {/* Input de Arquivo */}
            <div className="border-2 border-dashed border-neutral-300 p-6 text-center hover:border-black transition-colors">
              <input
                type="file"
                multiple
                accept="image/jpeg,image/png,image/webp"
                onChange={handlePhotoUpload}
                disabled={isCompressing || photos.length >= 8}
                className="hidden"
                id="scouting-photo-input"
              />
              <label
                htmlFor="scouting-photo-input"
                className="cursor-pointer block text-xs font-mono uppercase tracking-widest text-neutral-700"
              >
                {isCompressing
                  ? 'COMPRIMINDO IMAGENS NO NAVEGADOR...'
                  : photos.length >= 8
                  ? 'LIMITE DE 8 FOTOS ATINGIDO'
                  : '[ CLIQUE PARA SELECIONAR FOTOS ]'}
              </label>
              <span className="text-[10px] font-mono text-neutral-400 block mt-1">
                Processamento client-side com conversão para WebP de alto desempenho.
              </span>
            </div>

            {/* Grid de Pré-visualização com métricas de compressão */}
            {photos.length > 0 && (
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                {photos.map((item, idx) => (
                  <div key={idx} className="relative group border border-neutral-200 p-1 bg-neutral-50">
                    <img
                      src={item.previewUrl}
                      alt={item.label}
                      className="w-full aspect-[3/4] object-cover"
                    />
                    <div className="text-[9px] font-mono text-neutral-500 mt-1 flex justify-between items-center">
                      <span>{item.compressedSizeKb} KB</span>
                      <button
                        type="button"
                        onClick={() => removePhoto(idx)}
                        className="text-red-500 hover:text-red-700"
                      >
                        [REMOVER]
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Termos de Consentimento e LGPD Estrito */}
            <div className="border border-neutral-200 p-4 bg-neutral-50 text-xs font-sans text-neutral-600">
              <label className="flex items-start gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  required
                  checked={lgpdConsent}
                  onChange={(e) => setLgpdConsent(e.target.checked)}
                  className="mt-1 accent-black"
                />
                <span>
                  <strong>Consentimento Expresso LGPD (Lei nº 13.709/2018):</strong> Autorizo a{' '}
                  <strong>WB Scouting</strong> a tratar meus dados pessoais e biométricos fornecidos
                  neste formulário exclusivamente para finalidades de triagem de casting e agenciamento.
                  Estou ciente de que as fotos permanecerão em quarentena segura e serão eliminadas em
                  até 90 dias caso meu perfil não seja selecionado.
                </span>
              </label>
            </div>

            <div className="pt-6 flex justify-between items-center">
              <button
                type="button"
                onClick={() => setStep(3)}
                className="border border-neutral-300 px-6 py-2.5 text-xs font-mono uppercase tracking-widest text-neutral-700 hover:border-black"
              >
                &larr; Voltar
              </button>
              <button
                type="submit"
                disabled={isSubmitting || isCompressing || !lgpdConsent}
                className="bg-black text-white px-8 py-2.5 text-xs font-mono uppercase tracking-widest hover:bg-neutral-800 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isSubmitting ? 'ENVIANDO FICHA...' : 'CONCLUIR E ENVIAR'}
              </button>
            </div>
          </div>
        )}
      </form>
    </div>
  );
};
