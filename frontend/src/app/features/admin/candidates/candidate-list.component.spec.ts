import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { CandidateListComponent } from './candidate-list.component';
import { CandidateService } from '../../../core/services/candidate.service';
import { of, throwError } from 'rxjs';
import { Candidate, CandidatePageResponse } from '../../../core/models/candidate.model';
import { provideRouter } from '@angular/router';

describe('CandidateListComponent', () => {
  let component: CandidateListComponent;
  let fixture: ComponentFixture<CandidateListComponent>;
  let mockCandidateService: jasmine.SpyObj<CandidateService>;

  const mockCandidatesList: Candidate[] = [
    {
      id: 'c-1',
      fullName: 'Valentina Rocha',
      email: 'valentina@email.com',
      phone: '(11) 98888-7777',
      birthDate: '2007-04-12',
      isMinor: true,
      city: 'São Paulo',
      state: 'SP',
      height: 178,
      bust: 82,
      waist: 60,
      hips: 89,
      shoes: 38,
      eyeColor: 'Castanhos',
      hairColor: 'Castanho Escuro',
      status: 'PENDING',
      photos: [
        { id: 'p1', url: 'https://images.unsplash.com/photo-1.jpg', type: 'POLAROID_ROSTO' },
        { id: 'p2', url: 'https://images.unsplash.com/photo-2.jpg', type: 'POLAROID_PERFIL' }
      ],
      createdAt: '2026-09-28T14:32:00Z',
      age: 17
    },
    {
      id: 'c-2',
      fullName: 'Gabriel Castro',
      email: 'gabriel@email.com',
      phone: '(21) 99999-8888',
      birthDate: '2003-11-20',
      isMinor: false,
      city: 'Rio de Janeiro',
      state: 'RJ',
      height: 188,
      bust: 98,
      waist: 77,
      hips: 95,
      shoes: 42,
      eyeColor: 'Verdes',
      hairColor: 'Castanho Claro',
      status: 'APPROVED',
      photos: [
        { id: 'p3', url: 'https://images.unsplash.com/photo-3.jpg', type: 'POLAROID_ROSTO' }
      ],
      createdAt: '2026-09-27T10:15:00Z',
      age: 21
    }
  ];

  const mockPageResponse: CandidatePageResponse = {
    content: mockCandidatesList,
    totalElements: 2,
    totalPages: 1,
    size: 12,
    number: 0
  };

  beforeEach(async () => {
    mockCandidateService = jasmine.createSpyObj('CandidateService', [
      'getCandidates',
      'getCandidateById',
      'updateStatus',
      'promoteToModel'
    ]);

    mockCandidateService.getCandidates.and.returnValue(of(mockPageResponse));
    mockCandidateService.updateStatus.and.returnValue(of({
      ...mockCandidatesList[0],
      status: 'APPROVED'
    }));
    mockCandidateService.promoteToModel.and.returnValue(of({
      ...mockCandidatesList[0],
      status: 'APPROVED',
      convertedToModelId: 'mod-123'
    }));

    await TestBed.configureTestingModule({
      imports: [CandidateListComponent],
      providers: [
        provideRouter([]),
        { provide: CandidateService, useValue: mockCandidateService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CandidateListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve inicializar o componente e carregar candidaturas pendentes', () => {
    expect(component).toBeTruthy();
    expect(mockCandidateService.getCandidates).toHaveBeenCalled();
    expect(component.candidates().length).toBe(2);
    expect(component.activeStatusTab()).toBe('PENDING');
  });

  it('deve alternar abas de status e recarregar a lista', () => {
    component.setStatusTab('APPROVED');
    expect(component.activeStatusTab()).toBe('APPROVED');
    expect(component.currentPage()).toBe(0);
    expect(mockCandidateService.getCandidates).toHaveBeenCalledWith(jasmine.objectContaining({
      status: 'APPROVED'
    }));
  });

  it('deve buscar com debounce quando digitado no campo de busca', fakeAsync(() => {
    const inputEvent = { target: { value: 'Valentina' } } as unknown as Event;
    component.onSearchInput(inputEvent);

    tick(400); // aguarda debounceTime(350)
    expect(component.searchTerm()).toBe('Valentina');
    expect(mockCandidateService.getCandidates).toHaveBeenCalledWith(jasmine.objectContaining({
      search: 'Valentina'
    }));
  }));

  it('deve executar aprovação rápida com atualização otimista na interface', () => {
    const candidateToApprove = mockCandidatesList[0];
    component.quickApprove(candidateToApprove);

    // O status no candidate foi atualizado para APPROVED
    expect(mockCandidateService.updateStatus).toHaveBeenCalledWith('c-1', jasmine.objectContaining({
      status: 'APPROVED'
    }));
    expect(component.toast()?.type).toBe('success');
  });

  it('deve executar reprovação rápida com atualização otimista', () => {
    mockCandidateService.updateStatus.and.returnValue(of({
      ...mockCandidatesList[0],
      status: 'REJECTED'
    }));

    const candidateToReject = mockCandidatesList[0];
    component.quickReject(candidateToReject);

    expect(mockCandidateService.updateStatus).toHaveBeenCalledWith('c-1', jasmine.objectContaining({
      status: 'REJECTED'
    }));
    expect(component.toast()?.type).toBe('success');
  });

  it('deve abrir e fechar o drawer de dossiê do candidato', () => {
    expect(component.selectedCandidate()).toBeNull();

    component.openDrawer(mockCandidatesList[0]);
    expect(component.selectedCandidate()).toEqual(mockCandidatesList[0]);
    expect(component.activePhotoIndex()).toBe(0);

    component.selectPhoto(1);
    expect(component.activePhotoIndex()).toBe(1);

    component.closeDrawer();
    expect(component.selectedCandidate()).toBeNull();
  });

  it('deve salvar notas do scout com feedback de toast', () => {
    component.openDrawer(mockCandidatesList[0]);
    component.scoutNotes.set('Potencial incrível para passarela editorial.');

    component.saveScoutNotes();
    expect(mockCandidateService.updateStatus).toHaveBeenCalledWith('c-1', jasmine.objectContaining({
      notes: 'Potencial incrível para passarela editorial.'
    }));
    expect(component.toast()?.type).toBe('info');
  });

  it('deve promover candidato para o elenco quando confirmado', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.openDrawer(mockCandidatesList[0]);

    component.promoteToElenco();
    expect(mockCandidateService.promoteToModel).toHaveBeenCalledWith('c-1', true);
    expect(component.toast()?.type).toBe('success');
  });

  it('não deve promover se o usuário cancelar a confirmação', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    component.openDrawer(mockCandidatesList[0]);

    component.promoteToElenco();
    expect(mockCandidateService.promoteToModel).not.toHaveBeenCalled();
  });
});
