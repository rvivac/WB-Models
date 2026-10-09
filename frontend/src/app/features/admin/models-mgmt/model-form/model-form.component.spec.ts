import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModelFormComponent } from './model-form.component';
import { AdminModelService } from '../../../../core/services/admin-model.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { ModelAdminItem } from '../../../../shared/models/admin-model.interface';

describe('ModelFormComponent', () => {
  let component: ModelFormComponent;
  let fixture: ComponentFixture<ModelFormComponent>;
  let adminModelService: AdminModelService;
  let router: Router;

  const mockModel: ModelAdminItem = {
    id: 'uuid-123',
    stageName: 'Camila Pitanga',
    gender: 'FEMALE',
    isStar: true,
    isFeaturedHome: true,
    featuredOrder: 2,
    isActive: true,
    birthDate: '2000-01-01',
    heightCm: 178,
    bustChestCm: 86,
    waistCm: 61,
    hipsCm: 90,
    dressSize: '36',
    shoeSize: '38',
    city: 'Rio de Janeiro, RJ',
    nationality: 'Brasileira'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModelFormComponent],
      providers: [
        AdminModelService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'id' ? null : null)
              },
              url: []
            }
          }
        },
        {
          provide: Router,
          useValue: {
            navigate: jasmine.createSpy('navigate')
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ModelFormComponent);
    component = fixture.componentInstance;
    adminModelService = TestBed.inject(AdminModelService);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('deve instanciar o componente e inicializar formulário com valores padrão', () => {
    expect(component).toBeTruthy();
    expect(component.modelForm.get('gender')?.value).toBe('FEMALE');
    expect(component.modelForm.get('isStar')?.value).toBeFalse();
    expect(component.modelForm.get('isActive')?.value).toBeTrue();
  });

  it('deve marcar o formulário como inválido se o nome artístico estiver vazio', () => {
    component.modelForm.patchValue({ stageName: '' });
    expect(component.modelForm.invalid).toBeTrue();
  });

  it('deve calcular a idade dinamicamente a partir da data de nascimento', () => {
    component.modelForm.patchValue({ birthDate: '2000-01-01' });
    expect(component.computedAge()).toBeGreaterThanOrEqual(24);
  });

  it('deve atualizar computedMeasurements em tempo real', () => {
    component.modelForm.patchValue({
      heightCm: 180,
      bustChestCm: 88,
      waistCm: 62,
      hipsCm: 92
    });
    expect(component.computedMeasurements()).toContain('180 cm');
    expect(component.computedMeasurements()).toContain('88 / 62 / 92');
  });

  it('deve alternar gênero via setGender()', () => {
    component.setGender('MALE');
    expect(component.modelForm.get('gender')?.value).toBe('MALE');
    component.setGender('FEMALE');
    expect(component.modelForm.get('gender')?.value).toBe('FEMALE');
  });

  it('deve chamar createModel quando for um novo cadastro e emitir saved', () => {
    spyOn(adminModelService, 'createModel').and.returnValue(of(mockModel));
    spyOn(adminModelService, 'getModelMedia').and.returnValue(of([]));
    spyOn(component.saved, 'emit');

    component.modelForm.patchValue({
      stageName: 'Camila Pitanga',
      gender: 'FEMALE',
      isStar: true,
      heightCm: 178
    });

    component.onSubmit();

    expect(adminModelService.createModel).toHaveBeenCalled();
    expect(component.saved.emit).toHaveBeenCalledWith(mockModel);
  });

  it('deve chamar updateModel quando estiver em modo de edição com modelId', () => {
    spyOn(adminModelService, 'updateModel').and.returnValue(of(mockModel));
    spyOn(adminModelService, 'getModelMedia').and.returnValue(of([]));
    spyOn(component.saved, 'emit');

    component.modelId.set('uuid-123');
    component.modelForm.patchValue({
      stageName: 'Camila Pitanga Editada',
      gender: 'FEMALE'
    });

    component.onSubmit();

    expect(adminModelService.updateModel).toHaveBeenCalledWith('uuid-123', jasmine.any(Object));
    expect(component.saved.emit).toHaveBeenCalled();
  });

  it('deve carregar dados do modelo quando modelIdInput for informado', () => {
    spyOn(adminModelService, 'getModelById').and.returnValue(of(mockModel));

    component.modelIdInput = 'uuid-123';

    expect(adminModelService.getModelById).toHaveBeenCalledWith('uuid-123');
    expect(component.modelForm.get('stageName')?.value).toBe('Camila Pitanga');
    expect(component.modelForm.get('isStar')?.value).toBeTrue();
  });

  it('deve emitir cancelled ao clicar em cancelar', () => {
    spyOn(component.cancelled, 'emit');
    component.onCancel();
    expect(component.cancelled.emit).toHaveBeenCalled();
  });
});
