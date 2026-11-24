import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Society { id: string; name: string; locality: string; city: string; pinCode: string; address?: string; distanceKm?: number; }
export interface Membership { id: string; status: 'ACTIVE' | 'PENDING' | 'REJECTED'; societyId: string; unitNumber: string; }

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  searchSocieties(query: { city?: string; locality?: string; pinCode?: string; name?: string }): Observable<Society[]> {
    let params = new HttpParams();
    Object.entries(query).forEach(([key, value]) => { if (value) params = params.set(key, value); });
    return this.http.get<Society[]>('/api/societies/search', { params, withCredentials: true });
  }
  joinSociety(payload: { societyId: string; unitNumber: string; selfDeclared: boolean }): Observable<Membership> {
    return this.http.post<Membership>('/api/memberships', payload, { withCredentials: true });
  }
  activeMemberships(): Observable<Membership[]> {
    return this.http.get<Membership[]>('/api/memberships/me', { withCredentials: true });
  }
  nearbySocieties(latitude: number, longitude: number, radiusKm = 10): Observable<Society[]> {
    const params = new HttpParams().set('lat', latitude).set('lon', longitude).set('radiusKm', radiusKm);
    return this.http.get<Society[]>('/api/societies/nearby', { params, withCredentials: true });
  }
}
