import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonComponent } from '../../../shared/components/button/button.component';

@Component({
  selector: 'app-content-mgmt',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonComponent],
  templateUrl: './content-mgmt.component.html',
  styleUrls: ['./content-mgmt.component.scss']
})
export class ContentMgmtComponent {
  readonly isSaving = signal<boolean>(false);
  readonly saveSuccess = signal<boolean>(false);

  readonly contentForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.contentForm = this.fb.group({
      heroTitle: ['A elegância da moda com a precisão do scouting inteligente.', [Validators.required]],
      heroSubtitle: ['Conectamos os rostos mais autênticos e promissores às principais passarelas, campanhas globais e produções editoriais.', [Validators.required]],
      heroVideoUrl: ['https://assets.mixkit.co/videos/preview/mixkit-fashion-model-in-a-neon-lit-room-42861-large.mp4', [Validators.required]],
      agencyEmail: ['contato@wbscouting.com', [Validators.required, Validators.email]],
      agencyPhone: ['+55 (11) 99999-9999', [Validators.required]],
      agencyInstagram: ['@wbscouting', [Validators.required]]
    });
  }

  onSave(): void {
    if (this.contentForm.invalid) {
      return;
    }

    this.isSaving.set(true);
    this.saveSuccess.set(false);

    setTimeout(() => {
      this.isSaving.set(false);
      this.saveSuccess.set(true);
      setTimeout(() => this.saveSuccess.set(false), 3000);
    }, 800);
  }
}
