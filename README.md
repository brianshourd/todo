# TODO

## Run it

The application expects a Postgres database with the user `todo`, database
`todo`, and password `todo` running on `localhost:5432`. This can be easily
managed with docker:

```
docker run --name todo-postgres \
    -e POSTGRES_USER=todo \
    -e POSTGRES_PASSWORD=todo \
    -p 5432:5432 \
    -d postgres
```

Once the database is up and running, use `sbt run` to run the application, which
will be available at `localhost:9000`.

## Testing

The only tests that exist right now are end-to-end tests that require the
database to be running. Once a database is running, run

```
sbt test
```

to run the tests. Note that the tests will modify the database. Using the
`docker run` command above is recommended.

## Usage

### Create a Todo

Request:

```
POST /todos
{
  "text": "Feed the dog",
  "checked": false
}
```

Response:

```
201
{
  "id": 1,
  "text": "Feed the dog",
  "checked": false
}
```

### Get a Todo by id

Request:

```
GET /todos/1
```

Response:

```
200
{
  "id": 1,
  "text": "Feed the dog",
  "checked": false
}
```

### Update a Todo

Request:

```
PUT /todos/1
{
  "text": "Feed the cat",
  "checked": true
}
```

Response:

```
200
{
  "id": 1,
  "text": "Feed the cat",
  "checked": true
}
```

### Delete a Todo by id

Request:

```
DELETE /todos/1
```

Response:

```
200
{
  "id": 1,
  "text": "Feed the dog",
  "checked": false
}
```

### Check/Uncheck a Todo

It is possible to check a todo by performing an update, but update also updates
the text. To only perform a check:

Request:

```
POST /todos/1/check
```

or

```
POST /todos/1/uncheck
```

Response:

```
200
{
  "id": 1,
  "text": "Feed the dog",
  "checked": true
}
```

### Create a comment on a todo

Request:

```
POST /todos/1/comments
{
  "text": "Don't forget to take a leash"
}
```

Response:

```
201
{
  "id": 5,
  "text": "Don't forget to take a leash",
  "todoId": 1
}
```

### List all comments for a todo

Request:

```
GET /todos/1/comments
```

Response:

```
200
[
  {
    "id": 5,
    "text": "Don't forget to take a leash",
    "todoId": 1
  },
  {
    "id": 9,
    "text": "Just around the block",
    "todoId": 1
  }
]
```

## Limitations/Improvements

These were left out in the interest of time. If I were to actually ship this
code, all of these would need to be fixed before I would be satisfied.

* Error messages across the board are not good. I tried to return the
    appropriate http status codes in common circumstances, but the error
    messages need to be greatly improved. In addition, many error messages
    return html - ideally all error messages would respond with json.
* No support for pagination, or limitations around data access. If you add
    millions of items, the application will slow down and eventually break, and
    has no mechanisms built-in for recovery.
* Input parsing should be stricter. Currently parsing json to case classes
    simply ignores extraneous fields, but they should ideally be considered an
    error. There is also no upper limit on string length for string fields when
    parsing json, but a limit should be imposed.
* Tests are currently end-to-end, to ensure that the desired use cases are
    actually working. Long term, however, this testing method is slow and
    requires setting up Postgres out-of-band. The test suite should be augmented
    with unit tests as the modules become more complicated, and the end-to-end
    tests should take care of their own database.
