import { ChangeDetectorRef, Component, ViewChild } from '@angular/core';
import { UserComponent } from './user.component';
import { QueueComponent } from './queue.component';

@Component({
    selector: 'app-front',
    templateUrl: './front.component.html',
    styleUrl: './front.component.css',
    standalone: true,
    imports: [UserComponent, QueueComponent],
  })
  export class FrontPageComponent {
    username: string = '';
    passcode: string = '';
    
    constructor(private cdr: ChangeDetectorRef) {}
    @ViewChild(UserComponent) userComponent!: UserComponent;
    @ViewChild(QueueComponent) queueComponent!: QueueComponent;
    
    ngAfterViewInit(): void {
        this.cdr.detectChanges();
    }
  }