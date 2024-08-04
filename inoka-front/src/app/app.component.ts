import { ChangeDetectorRef, Component, ViewChild } from '@angular/core';
import { GameService } from './game.service';
import { UserComponent } from './user.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'inoka-front';

  username: string = '';

  constructor(private cdr: ChangeDetectorRef) {}
  @ViewChild(UserComponent) userComponent!: UserComponent;
  
  ngAfterViewInit() {
    this.cdr.detectChanges();
  }

  updateAndSavePlayer(newName: string) {
    this.username = newName;
    this.cdr.detectChanges();
    this.userComponent.updatePlayer();
  }
}
