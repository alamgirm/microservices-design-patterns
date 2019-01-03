import React, { Component } from 'react';
import { withRouter } from 'react-router-dom';
import { Button, Container, Form, FormGroup, Input, Label } from 'reactstrap';
import AppNavbar from './home/AppNavbar';
import { post } from './services/ApiService';
import MessageAlert from './MessageAlert';
import { errorMessage } from './common/Util';

class Login extends Component {
  login = {
    username: '',
    password: ''
  };

  constructor(props) {
    super(props);
    this.state = {
      login: this.login,
      displayError: null
    };
    this.validateForm = this.validateForm.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleChange = this.handleChange.bind(this);
  }

  validateForm() {
    return this.login.username.length > 0 && this.login.password.length > 0;
  }

  async handleSubmit(event) {
    event.preventDefault();
    const { login } = this.state;

    try {
      const data = await post('authenticate', login)
      if (data.id_token) {
        this.props.setAuthentication(data);
        this.props.history.push('/');
      } else {
        this.setState({ displayError: errorMessage(data)});
      }
    } catch (error) {
      this.setState({ displayError: errorMessage(error)});
    }
  }

  handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    let login = { ...this.state.login };
    login[name] = value;
    this.setState({ login });
  }

  render() {
    const { login, displayError } = this.state;

    return <div>
      <AppNavbar />
      <Container>
        <h2>Login</h2>
        <Form onSubmit={this.handleSubmit}>
          <FormGroup className="col-md-3 mb-3">
            <Label for="name">Username</Label>
            <Input type="text" name="username" id="username" onChange={this.handleChange} value={login.username || ''} />
          </FormGroup>
          <FormGroup className="col-md-3 mb-3">
            <Label for="password">Password</Label>
            <Input type="password" name="password" id="password" onChange={this.handleChange} value={login.password || ''} />
          </FormGroup>
          <FormGroup className="col-md-3 mb-3">
            <Button color="primary" block
              disabled={!this.validateForm}
              type="submit">Submit</Button>
          </FormGroup>
          <MessageAlert {...displayError}></MessageAlert>
        </Form>
      </Container>
    </div>
  }
}

export default withRouter(Login);