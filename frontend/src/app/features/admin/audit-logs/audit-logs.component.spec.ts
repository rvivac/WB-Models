import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuditLogsComponent } from './audit-logs.component';
import { AuditLogService, AdminAuditLogItem } from '../../../core/services/audit-log.service';
import { of, throwError } from 'rxjs';

describe('AuditLogsComponent', () => {
  let component: AuditLogsComponent;
  let fixture: ComponentFixture<AuditLogsComponent>;
  let auditLogServiceSpy: jasmine.SpyObj<AuditLogService>;

  const mockLogs: AdminAuditLogItem[] = [
    {
      id: '123e4567-e89b-12d3-a456-426614174000',
      adminEmail: 'webmaster@wbagency.com.br',
      action: 'UPDATE',
      resourceType: 'MODEL',
      resourceId: 'MOD-99',
      description: 'Atualização de modelo',
      detailsJson: { star: true },
      ipAddress: '189.10.20.30',
      userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0',
      createdAt: '2026-10-03T14:00:00Z'
    },
    {
      id: '223e4567-e89b-12d3-a456-426614174001',
      adminEmail: 'admin@wbagency.com.br',
      action: 'DECISION',
      resourceType: 'SCOUTING_CANDIDATE',
      resourceId: 'CAN-102',
      description: 'Aprovou candidatura',
      detailsJson: { status: 'APPROVED' },
      ipAddress: '200.50.60.70',
      userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Safari/605.1',
      createdAt: '2026-10-03T15:30:00Z'
    }
  ];

  beforeEach(async () => {
    auditLogServiceSpy = jasmine.createSpyObj('AuditLogService', ['getAuditLogs', 'getAuditLogById']);
    auditLogServiceSpy.getAuditLogs.and.returnValue(of({
      content: mockLogs,
      totalElements: 2,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: false
    }));

    await TestBed.configureTestingModule({
      imports: [AuditLogsComponent],
      providers: [
        { provide: AuditLogService, useValue: auditLogServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditLogsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load audit logs on initialization', () => {
    expect(component).toBeTruthy();
    expect(auditLogServiceSpy.getAuditLogs).toHaveBeenCalled();
    expect(component.logs().length).toBe(2);
    expect(component.totalElements()).toBe(2);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Trilha de Auditoria & Logs');
    expect(compiled.textContent).toContain('webmaster@wbagency.com.br');
    expect(compiled.textContent).toContain('admin@wbagency.com.br');
    expect(compiled.textContent).toContain('UPDATE');
    expect(compiled.textContent).toContain('DECISION');
  });

  it('should filter logs when applyFilters is called', () => {
    component.filterForm.patchValue({
      search: 'MOD-99',
      action: 'UPDATE'
    });

    component.applyFilters();
    expect(auditLogServiceSpy.getAuditLogs).toHaveBeenCalledTimes(2);
  });

  it('should clear filters and reload', () => {
    component.filterForm.patchValue({
      search: 'TEST',
      action: 'DELETE'
    });

    component.clearFilters();
    expect(component.filterForm.get('search')?.value).toBe('');
    expect(component.filterForm.get('action')?.value).toBe('');
  });

  it('should open and close details modal', () => {
    expect(component.isDetailsModalOpen()).toBeFalse();
    component.openDetailsModal(mockLogs[0]);
    expect(component.isDetailsModalOpen()).toBeTrue();
    expect(component.selectedLog()?.id).toBe(mockLogs[0].id);

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('MOD-99');
    expect(compiled.textContent).toContain('189.10.20.30');

    component.closeDetailsModal();
    expect(component.isDetailsModalOpen()).toBeFalse();
    expect(component.selectedLog()).toBeNull();
  });

  it('should display error message when service fails', () => {
    auditLogServiceSpy.getAuditLogs.and.returnValue(throwError(() => ({
      error: { detail: 'Acesso restrito ao Webmaster' }
    })));

    component.loadLogs();
    expect(component.errorMessage()).toBe('Acesso restrito ao Webmaster');
  });
});
