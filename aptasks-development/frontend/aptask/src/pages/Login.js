import React, { useState } from 'react';
import { useNavigate, Link } from "react-router-dom";
import Img from "../imagem/ap103.png";
import '../style/Login.css';
import { loginAdmin } from '../services/api';

// Acesso restrito ao administrador: o e-mail do morador Ezequiel, conferido pelo backend.
const Login = ({ onAuthenticated }) => {
    const [email, setEmail] = useState('');
    const [error, setError] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');
        setSubmitting(true);
        try {
            const session = await loginAdmin(email.trim());
            onAuthenticated?.(session.token);
            navigate('/home');
        } catch (err) {
            setError(err?.response?.data?.error || 'Não foi possível entrar. Tente novamente.');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div style={styles.container}>
            <div className="logo"><Link to="/"><img src={Img} alt="logo da página" /></Link></div>
            <h2>Acesso do administrador</h2>
            {error && <p role="alert" style={{ color: 'red' }}>{error}</p>}
            <form style={styles.form} onSubmit={handleLogin}>
                <div style={styles.inputGroup}>
                    <label htmlFor="admin-email">Email:</label>
                    <input
                        type="email"
                        id="admin-email"
                        autoComplete="username"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                        style={styles.input}
                    />
                </div>
                <button type="submit" disabled={submitting} style={styles.button}>
                    {submitting ? 'Entrando…' : 'Entrar'}
                </button>
            </form>
            <p style={{ textAlign: 'center' }}><Link to="/home">Voltar ao início</Link></p>
        </div>
    );
};

const styles = {
    container: { maxWidth: 360, margin: '48px auto', padding: 24, fontFamily: 'sans-serif' },
    form: { display: 'flex', flexDirection: 'column', gap: 12 },
    inputGroup: { display: 'flex', flexDirection: 'column', gap: 4 },
    input: { padding: 8, fontSize: 14 },
    button: { padding: 10, fontSize: 14, cursor: 'pointer' },
};

export default Login;
