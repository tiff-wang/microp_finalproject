import { GraphPage } from './app.po';

describe('graph App', () => {
  let page: GraphPage;

  beforeEach(() => {
    page = new GraphPage();
  });

  it('should display welcome message', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('Welcome to app!!');
  });
});
