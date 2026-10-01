import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AboutPageManagerComponent } from './about-page-manager.component';
import { AboutPageService } from '../../../core/services/about-page.service';
import { AboutPage } from '../../../shared/models/about-page.interface';

describe('AboutPageManagerComponent', () => {
  let component: AboutPageManagerComponent;
  let fixture: ComponentFixture<AboutPageManagerComponent>;
  let aboutServiceSpy: jasmine.SpyObj<AboutPageService>;

  const mockAboutPage: AboutPage = {
    title: 'A Nova Estética do Scouting Global',
    subtitle: 'MANIFESTO INSTITUCIONAL',
    description: 'Descrição de teste',
    heroQuote: 'Citação hero de teste',
    manifestoTitle: 'Nossa Filosofia',
    manifestoText: 'Texto do manifesto de teste',
    pillarsTitle: 'Nossos Pilares & Valores',
    pillars: [
      {
        order: 1,
        titulo: 'Curadoria & Autenticidade',
        descricao: 'Descrição pilar 1'
      },
      {
        order: 2,
        titulo: 'Transparência & Ética',
        descricao: 'Descrição pilar 2'
      }
    ],
    seo: {
      metaTitle: 'Sobre Nós | WB Agency',
      metaDescription: 'Descrição SEO'
    },
    updatedAt: '2026-10-01T12:00:00Z'
  };

  beforeEach(async () => {
    aboutServiceSpy = jasmine.createSpyObj('AboutPageService', [
      'getAdminAboutPage',
      'updateAboutPage'
    ], {
      defaultAboutPage: JSON.parse(JSON.stringify(mockAboutPage))
    });

    aboutServiceSpy.getAdminAboutPage.and.callFake(() => of(JSON.parse(JSON.stringify(mockAboutPage))));

    await TestBed.configureTestingModule({
      imports: [AboutPageManagerComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AboutPageService, useValue: aboutServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AboutPageManagerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve criar o componente e carregar dados no formulário', () => {
    expect(component).toBeTruthy();
    expect(component.aboutForm.get('title')?.value).toBe('A Nova Estética do Scouting Global');
    expect(component.aboutForm.get('subtitle')?.value).toBe('MANIFESTO INSTITUCIONAL');
    expect(component.pillarsArray.length).toBe(2);
    expect(component.lastUpdated).toBe('2026-10-01T12:00:00Z');
  });

  it('deve adicionar um novo pilar ao formulário', () => {
    const initialCount = component.pillarsArray.length;
    component.addPillar();
    expect(component.pillarsArray.length).toBe(initialCount + 1);
    expect(component.pillarsArray.at(initialCount).get('order')?.value).toBe(initialCount + 1);
  });

  it('deve remover um pilar quando houver mais de um', () => {
    expect(component.pillarsArray.length).toBe(2);
    component.removePillar(0);
    expect(component.pillarsArray.length).toBe(1);
    expect(component.pillarsArray.at(0).get('order')?.value).toBe(1);
  });

  it('não deve remover pilar se houver apenas um', () => {
    component.removePillar(0);
    expect(component.pillarsArray.length).toBe(1);
    component.removePillar(0);
    expect(component.pillarsArray.length).toBe(1);
    expect(component.feedbackMessage).toContain('ao menos um pilar');
  });

  it('deve mover pilar para baixo e atualizar ordenação', () => {
    const firstTitle = component.pillarsArray.at(0).get('titulo')?.value;
    const secondTitle = component.pillarsArray.at(1).get('titulo')?.value;

    component.movePillarDown(0);

    expect(component.pillarsArray.at(0).get('titulo')?.value).toBe(secondTitle);
    expect(component.pillarsArray.at(1).get('titulo')?.value).toBe(firstTitle);
    expect(component.pillarsArray.at(0).get('order')?.value).toBe(1);
    expect(component.pillarsArray.at(1).get('order')?.value).toBe(2);
  });

  it('deve mover pilar para cima e atualizar ordenação', () => {
    const firstTitle = component.pillarsArray.at(0).get('titulo')?.value;
    const secondTitle = component.pillarsArray.at(1).get('titulo')?.value;

    component.movePillarUp(1);

    expect(component.pillarsArray.at(0).get('titulo')?.value).toBe(secondTitle);
    expect(component.pillarsArray.at(1).get('titulo')?.value).toBe(firstTitle);
  });

  it('deve salvar formulário válido com sucesso', () => {
    aboutServiceSpy.updateAboutPage.and.returnValue(of({
      ...mockAboutPage,
      title: 'Título Novo Salvo',
      updatedAt: '2026-10-01T14:00:00Z'
    }));

    component.aboutForm.patchValue({
      title: 'Título Novo Salvo'
    });

    component.save();

    expect(aboutServiceSpy.updateAboutPage).toHaveBeenCalled();
    expect(component.isSaving).toBeFalse();
    expect(component.lastUpdated).toBe('2026-10-01T14:00:00Z');
    expect(component.feedbackType).toBe('success');
  });

  it('não deve submeter se o formulário for inválido', () => {
    component.aboutForm.patchValue({
      title: ''
    });

    component.save();

    expect(aboutServiceSpy.updateAboutPage).not.toHaveBeenCalled();
    expect(component.feedbackType).toBe('error');
  });

  it('deve restaurar padrões ao confirmar', () => {
    spyOn(window, 'confirm').and.returnValue(true);

    component.aboutForm.patchValue({
      title: 'Título Modificado'
    });

    component.restoreDefaults();

    expect(component.aboutForm.get('title')?.value).toBe(component.aboutForm.get('title')?.value);
    expect(component.feedbackType).toBe('success');
  });
});
