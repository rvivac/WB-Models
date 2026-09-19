import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModelDetailPublicDto } from '../../../../../../../core/services/public-model.service';
import { TranslatePipe } from '../../../../../../../shared/pipes/translate.pipe';

export interface MeasurementItem {
  labelKey: string;
  value: string;
}

@Component({
  selector: 'app-model-measurements',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './model-measurements.component.html',
  styleUrls: ['./model-measurements.component.scss']
})
export class ModelMeasurementsComponent {
  readonly model = input.required<ModelDetailPublicDto>();

  // Lista dinâmica de medidas com supressão estrita de campos nulos/vazios
  readonly measurementList = computed<MeasurementItem[]>(() => {
    const m = this.model();
    const list: MeasurementItem[] = [];

    if (m.heightCm != null && m.heightCm > 0) {
      list.push({ labelKey: 'model_detail.height', value: `${m.heightCm} cm` });
    }
    if (m.bustChestCm != null && Number(m.bustChestCm) > 0) {
      list.push({ labelKey: 'model_detail.bust_chest', value: `${m.bustChestCm} cm` });
    }
    if (m.waistCm != null && Number(m.waistCm) > 0) {
      list.push({ labelKey: 'model_detail.waist', value: `${m.waistCm} cm` });
    }
    if (m.hipsCm != null && Number(m.hipsCm) > 0) {
      list.push({ labelKey: 'model_detail.hips', value: `${m.hipsCm} cm` });
    }
    if (m.dressSize && m.dressSize.trim().length > 0) {
      list.push({ labelKey: 'model_detail.dress', value: m.dressSize.trim() });
    }
    if (m.shoeSize && m.shoeSize.trim().length > 0) {
      list.push({ labelKey: 'model_detail.shoe', value: m.shoeSize.trim() });
    }
    if (m.eyeColor && m.eyeColor.trim().length > 0) {
      list.push({ labelKey: 'model_detail.eyes', value: m.eyeColor.trim() });
    }
    if (m.hairColor && m.hairColor.trim().length > 0) {
      list.push({ labelKey: 'model_detail.hair', value: m.hairColor.trim() });
    }

    return list;
  });
}
