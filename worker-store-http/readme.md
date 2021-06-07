# worker-store-http

---

 This is an implementation of a `DataStore` that reads and writes data from
 and to a HTTP server.

## Configuration

 The configuration source for this module is `HttpDataStoreConfiguration`.
 The following configuration options are present:

 - url: the url of the HTTP server to be used to store and read
  data. This HTTP server must support anonymous access. This
  parameter must not be null and must not be empty.
 - connectTimeoutMillis: the timeout value, in milliseconds, to be used
  when opening a communications link to the HTTP server. Default value
  is 10000 (10 seconds). A value of 0 (infinite timeout) is not allowed.
 - readTimeoutMillis: the timeout value, in milliseconds, to be used
  when reading from the HTTP server after a connection is established.
  Default value is 10000 (10 seconds). A value of 0 (infinite timeout)
  is not allowed.

## Usage

 This is a simple module (and as such may be suitable for unit or integration
 tests) which will simply be looking for files on a HTTP server.

## Failure modes

 The following scenarios will prevent the module from initialising:

 - The url was not specified

 The following scenarios have been identified as possible runtime failure modes
 for this module:

 - HTTP server unavailable
 - Connect/read timeout when communicating with HTTP server
 - HTTP server does not support anonymous access
 - HTTP server does not support one or more of the HTTP methods required (GET, PUT, DELETE)
 - Insufficient disk space on HTTP server

## Maintainers

The following people are responsible for maintaining this code:

- Andy Reid (Belfast, UK, andrew.reid@microfocus.com)
- Dermot Hardy (Belfast, UK, dermot.hardy@microfocus.com)
- Michael Bryson (Belfast, UK, michael.bryson@microfocus.com)
- Rory Torney (Belfast, UK, rory.torney@microfocus.com)
- Kusuma Ghosh Dastidar (Pleasanton, USA, vgkusuma@microfocus.com)
- David Milligan (Belfast, UK, davidgerald.milligan@microfocus.com)
- Xavier Lamourec (Belfast, UK, xavier.lamourec@microfocus.com)
- Kleyton O'Hare (Belfast, UK, kleyton.ohare@microfocus.com)
- Simranjeet Singh Chawla (Belfast, UK, simranjeet.singhchawla@microfocus.com)
