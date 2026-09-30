import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PhotoUploaderGridComponent } from './photo-uploader-grid.component';
import { GalleryPhoto } from '../../../../../shared/models/gallery.model';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

describe('PhotoUploaderGridComponent', () => {
  let component: PhotoUploaderGridComponent;
  let fixture: ComponentFixture<PhotoUploaderGridComponent>;

  const initialPhotos: GalleryPhoto[] = [
    {
      id: 'photo-1',
      url: 'https://images.unsplash.com/photo-1',
      category: 'BOOK',
      orderIndex: 0,
      isCover: true
    },
    {
      id: 'photo-2',
      url: 'https://images.unsplash.com/photo-2',
      category: 'POLAROID',
      orderIndex: 1,
      isCover: false
    },
    {
      id: 'photo-3',
      url: 'https://images.unsplash.com/photo-3',
      category: 'BOOK',
      orderIndex: 2,
      isCover: false
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PhotoUploaderGridComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(PhotoUploaderGridComponent);
    component = fixture.componentInstance;
    component.photos = JSON.parse(JSON.stringify(initialPhotos));
    fixture.detectChanges();
  });

  it('deve instanciar o componente corretamente', () => {
    expect(component).toBeTruthy();
    expect(component.photos.length).toBe(3);
  });

  it('a primeira foto (índice 0) deve ser obrigatoriamente a Capa Principal', () => {
    expect(component.photos[0].isCover).toBeTrue();
    expect(component.photos[1].isCover).toBeFalse();
  });

  it('deve promover foto para capa e recalcular índices ao chamar setAsCover()', () => {
    spyOn(component.photosChange, 'emit');

    // Promove a foto do índice 2 para capa
    component.setAsCover(2);

    expect(component.photos[0].id).toBe('photo-3');
    expect(component.photos[0].isCover).toBeTrue();
    expect(component.photos[0].orderIndex).toBe(0);
    expect(component.photos[1].id).toBe('photo-1');
    expect(component.photos[1].isCover).toBeFalse();
    expect(component.photos[1].orderIndex).toBe(1);
    expect(component.photosChange.emit).toHaveBeenCalled();
  });

  it('deve remover foto e atualizar os índices sequenciais ao chamar removePhoto()', () => {
    spyOn(component.photosChange, 'emit');

    component.removePhoto(0);

    expect(component.photos.length).toBe(2);
    expect(component.photos[0].id).toBe('photo-2');
    expect(component.photos[0].isCover).toBeTrue();
    expect(component.photos[0].orderIndex).toBe(0);
    expect(component.photosChange.emit).toHaveBeenCalled();
  });

  it('deve alternar categoria entre BOOK e POLAROID ao chamar toggleCategory()', () => {
    spyOn(component.photosChange, 'emit');

    const photo = component.photos[0];
    expect(photo.category).toBe('BOOK');

    component.toggleCategory(photo);
    expect(photo.category).toBe('POLAROID');
    expect(component.photosChange.emit).toHaveBeenCalled();

    component.toggleCategory(photo);
    expect(photo.category).toBe('BOOK');
  });

  it('deve reordenar a galeria e atualizar índices via onReorder()', () => {
    spyOn(component.photosChange, 'emit');

    const mockDropEvent = {
      previousIndex: 0,
      currentIndex: 1,
      item: {} as any,
      container: {} as any,
      previousContainer: {} as any,
      isPointerOverContainer: true,
      distance: { x: 0, y: 0 },
      dropPoint: { x: 0, y: 0 },
      event: new MouseEvent('drop')
    } as CdkDragDrop<GalleryPhoto[]>;

    component.onReorder(mockDropEvent);

    expect(component.photos[0].id).toBe('photo-2');
    expect(component.photos[0].orderIndex).toBe(0);
    expect(component.photos[0].isCover).toBeTrue();
    expect(component.photos[1].id).toBe('photo-1');
    expect(component.photos[1].orderIndex).toBe(1);
    expect(component.photos[1].isCover).toBeFalse();
    expect(component.photosChange.emit).toHaveBeenCalled();
  });

  it('deve filtrar e validar arquivos válidos e emitir fileUploaded', () => {
    spyOn(component.fileUploaded, 'emit');

    const validBlob = new Blob(['sample-img'], { type: 'image/jpeg' });
    const validFile = new File([validBlob], 'foto1.jpg', { type: 'image/jpeg' });

    const invalidTypeBlob = new Blob(['sample-pdf'], { type: 'application/pdf' });
    const invalidTypeFile = new File([invalidTypeBlob], 'doc.pdf', { type: 'application/pdf' });

    const fileList = {
      0: validFile,
      1: invalidTypeFile,
      length: 2,
      item: (i: number) => (i === 0 ? validFile : invalidTypeFile)
    } as unknown as FileList;

    component.processSelectedFiles(fileList);

    expect(component.fileUploaded.emit).toHaveBeenCalledWith([validFile]);
  });

  it('deve emitir erro de validação para arquivos com mais de 10MB', () => {
    spyOn(component.validationError, 'emit');

    const largeFile = new File([new ArrayBuffer(11 * 1024 * 1024)], 'large.jpg', {
      type: 'image/jpeg'
    });

    const fileList = {
      0: largeFile,
      length: 1,
      item: () => largeFile
    } as unknown as FileList;

    component.processSelectedFiles(fileList);

    expect(component.validationError.emit).toHaveBeenCalledWith(
      'O tamanho do arquivo excede o limite máximo permitido de 10 MB.'
    );
  });

  it('deve alterar o filtro de categoria selecionado', () => {
    component.selectedCategoryFilter = 'BOOK';
    expect(component.selectedCategoryFilter).toBe('BOOK');

    component.selectedCategoryFilter = 'POLAROID';
    expect(component.selectedCategoryFilter).toBe('POLAROID');

    component.selectedCategoryFilter = 'ALL';
    expect(component.selectedCategoryFilter).toBe('ALL');
  });
});
