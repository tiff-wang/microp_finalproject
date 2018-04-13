import { Component, OnInit, ViewChild, ElementRef} from '@angular/core';
import * as _ from 'lodash'
import { AppService} from './app.service'

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit{
  title = 'app';

  @ViewChild('chart') el: ElementRef;

  constructor(private appService: AppService){
  }

  ngOnInit(){
    this.basicChart();

  }

  basicChart(){
    const element = this.el.nativeElement

    const data = [{
      x: [1, 2, 3, 4, 5],
      y: [1, 2, 3, 4, 5]
    }]

    const style = {
      margin: { t: 0}
    }

    Plotly.plot(element, data, style);
  }

  realtime(data) {
    const element = this.el.nativeElement
    const formattedData = [{
      z: data,
      type: 'surface'
    }];

    const layout = {
      title: 'Mt Bruno Elevation',
      autosize: false,
      width: 750,
      height: 500,
      margin: {
        l: 65,
        r: 50,
        b: 65,
        t: 90,
      }
    };

    Plotly.plot(element, data, layout);
  }
}
