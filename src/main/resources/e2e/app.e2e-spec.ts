import { ResourcesPage } from './app.po';

describe('resources App', function() {
  let page: ResourcesPage;

  beforeEach(() => {
    page = new ResourcesPage();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
