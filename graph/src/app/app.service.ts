import { Injectable } from '@angular/core';
import { AngularFireDatabase, AngularFireObject } from 'angularfire2/database';



@Injectable()
export class AppService {

  constructor(private db: AngularFireDatabase) { }

  getData(dataset: string) {
    return this.db.list(dataset)
  }


  // Used for realtime data updates
  updateRanking(key, newData) {
    return this.db.object(`rankings/${key}`).update(newData)
  }

}
