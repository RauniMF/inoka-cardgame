import { HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const http = inject(HttpClient);
  
  const authToken = localStorage.getItem('authToken');
  
  if (req.url.includes('/player/add') || req.url.includes('/player/refresh-token')) {
    return next(req);
  }
  
  // Clone the request and add the authorization header if token exists
  if (authToken) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${authToken}`
      }
    });
  }
  
  // Handle the request and catch 401 errors
  return next(req).pipe(
    catchError((error) => {
      // Only attempt refresh on 401 with a token
      if (error.status === 401 && authToken) {
        console.log('Token expired, attempting to refresh...');
        
        const userUUID = localStorage.getItem('userUUID');
        
        // If we have a UUID, try to refresh the token
        if (userUUID) {
          return http.post<{ player: any, token: string }>(
            'http://localhost:8080/inoka/player/refresh-token',
            { playerId: userUUID }
          ).pipe(
            switchMap((response) => {
              // Store the new token
              localStorage.setItem('authToken', response.token);
              console.log('Token refreshed successfully');
              
              // Clone the original request with the new token
              const clonedRequest = req.clone({
                setHeaders: {
                  Authorization: `Bearer ${response.token}`
                }
              });
              
              // Retry the original request with the new token
              return next(clonedRequest);
            }),
            catchError((refreshError) => {
              // Refresh failed - let the service handle cleanup
              console.log('Token refresh failed, letting service handle cleanup', refreshError);
              return throwError(() => refreshError);
            })
          );
        } else {
          // No UUID stored, return the error
          console.log('No UUID found for token refresh');
          return throwError(() => error);
        }
      }
      
      // For non-401 errors, just pass them through
      return throwError(() => error);
    })
  );
};
