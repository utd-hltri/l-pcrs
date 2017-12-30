# Let's get this party started!
import falcon
from wsgiref import simple_server


# Falcon follows the REST architectural style, meaning (among
# other things) that you think in terms of resources and state
# transitions, which map to HTTP verbs.
class Resource(object):
  def on_get(self, req, resp):
    """Handles GET requests"""
    resp.status = falcon.HTTP_200  # This is the default status
    resp.body = ('\nTwo things awe me most, the starry sky '
                 'above me and the moral law within me.\n'
                 '\n'
                 '    ~ Immanuel Kant\n\n')


# falcon.API instances are callable WSGI apps
app = falcon.API()

# Resources are represented by long-lived class instances
r = Resource()

# things will handle all requests to the '/things' URL path
app.add_route('/things', r)

if __name__ == '__main__':
    httpd = simple_server.make_server('0.0.0.0', 8050, app)
    httpd.serve_forever()
