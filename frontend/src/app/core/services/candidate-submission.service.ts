import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CandidateSubmissionModel,
  CandidateSubmissionPhotos,
  CandidateSubmissionResponse
} from '../../shared/models/candidate-submission.interface';

@Injectable({
  providedIn: 'root'
})
export class CandidateSubmissionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  /**
   * Envia uma candidatura completa multipart/form-data com o JSON de dados e as 3 fotos obrigatórias.
   */
  submit(
    data: CandidateSubmissionModel,
    photos: CandidateSubmissionPhotos | { face: File; profile: File; fullBody: File }
  ): Observable<CandidateSubmissionResponse> {
    const formData = new FormData();

    // Injeção do Blob JSON para a part 'data' esperada pelo controller Spring Boot
    const jsonBlob = new Blob([JSON.stringify(data)], { type: 'application/json' });
    formData.append('data', jsonBlob);

    // Mapeamento flexível dos arquivos de foto
    const faceFile = 'facePhoto' in photos ? photos.facePhoto : photos.face;
    const profileFile = 'profilePhoto' in photos ? photos.profilePhoto : photos.profile;
    const fullBodyFile = 'fullBodyPhoto' in photos ? photos.fullBodyPhoto : photos.fullBody;

    if (faceFile) {
      formData.append('facePhoto', faceFile, faceFile.name);
    }
    if (profileFile) {
      formData.append('profilePhoto', profileFile, profileFile.name);
    }
    if (fullBodyFile) {
      formData.append('fullBodyPhoto', fullBodyFile, fullBodyFile.name);
    }

    return this.http.post<CandidateSubmissionResponse>(`${this.baseUrl}/submissions`, formData);
  }
}
