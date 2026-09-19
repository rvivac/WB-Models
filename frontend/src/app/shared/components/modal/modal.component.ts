import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './modal.component.html',
  styleUrls: ['./modal.component.scss']
})
export class ModalComponent {
  readonly isOpen = input<boolean>(false);
  readonly title = input<string>('');
  readonly close = output<void>();

  onClose(): void {
    this.close.emit();
  }
}
