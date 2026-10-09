import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { ContactSettingsComponent, ContactSettingsData } from './contact-settings.component';
import { environment } from '../../../../environments/environment';

describe('ContactSettingsComponent', () => {
  let component: ContactSettingsComponent;
  let fixture: ComponentFixture<ContactSettingsComponent>;
  let httpMock: HttpTestingController;

  const mockContactData: ContactSettingsData = {
    primaryEmail: 'contato@wbscouting.com',
    scoutingEmail: 'scouting@wbscouting.com',
    pressEmail: 'press@wbscouting.com',
    phone: '+55 11 99999-9999',
    whatsapp: '+55 11 99999-9999',
    whatsappDefaultMessage: 'Olá! Gostaria de falar com a equipe de atendimento da WB Agency.',
    businessHours: 'Segunda a Sexta: 09h às 18h (GMT-3)',
    address: {
      street: 'Avenida Paulista, 1000',
      complement: 'Conjunto 1402',
      neighborhood: 'Bela Vista',
      city: 'São Paulo',
      state: 'SP',
      zipCode: '01310-100',
      country: 'Brasil'
    },
    socialMedia: {
      instagram: 'https://instagram.com/wbagency',
      facebook: ''
    },
    socialMediaList: [
      { id: 'soc-1', name: 'LinkedIn', url: 'https://linkedin.com/company/wbagency' },
      { id: 'soc-2', name: 'TikTok', url: 'https://tiktok.com/@wbagency' }
    ]
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ContactSettingsComponent,
        HttpClientTestingModule,
        FormsModule,
        RouterTestingModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ContactSettingsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve inicializar e carregar dados de contato', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/institutional/contact`);
    expect(req.request.method).toBe('GET');
    req.flush(mockContactData);

    expect(component).toBeTruthy();
    expect(component.contactData.primaryEmail).toBe('contato@wbscouting.com');
    expect(component.contactData.whatsapp).toBe('+55 11 99999-9999');
    expect(component.editingField).toBeNull();
  });

  it('deve iniciar e cancelar o modo de edição de um campo', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/contact`).flush(mockContactData);

    component.startEdit('whatsapp', component.contactData.whatsapp);
    expect(component.editingField).toBe('whatsapp');
    expect(component.tempValue).toBe('+55 11 99999-9999');

    component.cancelEdit();
    expect(component.editingField).toBeNull();
    expect(component.tempValue).toBe('');
  });

  it('deve salvar campo individual via PATCH com sucesso e exibir feedback', fakeAsync(() => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/contact`).flush(mockContactData);

    component.startEdit('primaryEmail', 'novo@wbscouting.com');
    component.tempValue = 'novo@wbscouting.com';
    component.saveField('primaryEmail');

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/institutional/contact`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ primaryEmail: 'novo@wbscouting.com' });
    req.flush({ ...mockContactData, primaryEmail: 'novo@wbscouting.com' });

    expect(component.contactData.primaryEmail).toBe('novo@wbscouting.com');
    expect(component.editingField).toBeNull();
    expect(component.successField).toBe('primaryEmail');

    tick(3000);
    expect(component.successField).toBeNull();
  }));

  it('deve salvar rede social aninhada via PATCH', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/contact`).flush(mockContactData);

    component.startEdit('socialMedia.instagram', 'https://instagram.com/wbnovoperfil');
    component.tempValue = 'https://instagram.com/wbnovoperfil';
    component.saveField('socialMedia.instagram');

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/institutional/contact`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.socialMedia.instagram).toBe('https://instagram.com/wbnovoperfil');
    req.flush({
      ...mockContactData,
      socialMedia: { ...mockContactData.socialMedia, instagram: 'https://instagram.com/wbnovoperfil' }
    });

    expect(component.contactData.socialMedia?.instagram).toBe('https://instagram.com/wbnovoperfil');
  });

  it('deve sanitizar números e codificar texto para preview do WhatsApp', () => {
    expect(component.cleanNumber('+55 (11) 99999-8888')).toBe('5511999998888');
    expect(component.encodeText('Olá mundo!')).toBe('Ol%C3%A1%20mundo!');
  });

  it('deve adicionar um novo item de rede social no modo de edição', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/contact`).flush(mockContactData);

    const initialCount = component.socialMediaList.length;
    component.addSocialMedia();

    expect(component.socialMediaList.length).toBe(initialCount + 1);
    const added = component.socialMediaList[component.socialMediaList.length - 1];
    expect(added.name).toBe('');
    expect(added.url).toBe('');
    expect(added.isEditing).toBeTrue();
  });

  it('deve salvar item de rede social e disparar persistência via PATCH', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/contact`).flush({
      ...mockContactData,
      socialMediaList: []
    });

    component.addSocialMedia();
    const item = component.socialMediaList[0];
    item.name = 'YouTube';
    item.url = 'https://youtube.com/@wbagency';

    component.saveSocialMediaItem(item);
    expect(item.isEditing).toBeFalse();

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/institutional/contact`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.socialMediaList).toEqual([
      { id: undefined, name: 'YouTube', url: 'https://youtube.com/@wbagency' }
    ]);
    req.flush({
      ...mockContactData,
      socialMediaList: [{ id: 'soc-1', name: 'YouTube', url: 'https://youtube.com/@wbagency' }]
    });

    expect(component.socialMediaList.length).toBe(1);
    expect(component.socialMediaList[0].id).toBe('soc-1');
  });

  it('deve remover item de rede social e atualizar via PATCH', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/contact`).flush({
      ...mockContactData,
      socialMediaList: [
        { id: '1', name: 'Twitter', url: 'https://x.com/wb' },
        { id: '2', name: 'TikTok', url: 'https://tiktok.com/@wb' }
      ]
    });

    expect(component.socialMediaList.length).toBe(2);

    component.removeSocialMedia(0);

    const req = httpMock.expectOne(`${environment.apiUrl}/admin/institutional/contact`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.socialMediaList).toEqual([
      { id: '2', name: 'TikTok', url: 'https://tiktok.com/@wb' }
    ]);
    req.flush({
      ...mockContactData,
      socialMediaList: [{ id: '2', name: 'TikTok', url: 'https://tiktok.com/@wb' }]
    });

    expect(component.socialMediaList.length).toBe(1);
    expect(component.socialMediaList[0].name).toBe('TikTok');
  });

  it('deve cancelar edição de rede social e descartar item se estiver vazio', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${environment.apiUrl}/admin/institutional/contact`).flush({
      ...mockContactData,
      socialMediaList: []
    });

    component.addSocialMedia();
    const item = component.socialMediaList[0];
    expect(component.socialMediaList.length).toBe(1);

    component.cancelSocialMediaEdit(item, 0);
    expect(component.socialMediaList.length).toBe(0);
  });
});
