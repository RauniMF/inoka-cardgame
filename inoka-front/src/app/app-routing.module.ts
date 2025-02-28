import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => {
        return import('./components/front-page/front.component').then((m) => m.FrontPageComponent)
    },
  },
  {
    path: 'lobby',
    loadComponent: () => {
      return import('./components/lobby/lobby.component').then((m) => m.LobbyComponent)
    }
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
